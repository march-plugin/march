# MARCH - Maven ARCHitecture integrity enforcement plugin

A Maven plugin that enforces your intended architecture automatically, as part of the build.

## The idea

Multi-module Java projects usually have an intended architecture in mind: layers, domains,
api vs. impl, ports & adapters, whatever it may be. Nothing stops developers from quietly
violating it, and reviewers can't catch every cross-module dependency by eye. march lets you
declare the architecture once, and enforces it automatically on every build, on two levels:

- the Maven module graph (`pom.xml` dependencies)
- the actual compiled bytecode, using ArchUnit

## Quick example

Add the plugin to your root `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.march-plugin</groupId>
            <artifactId>march-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>validate</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <configFile>march-config.xml</configFile>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Describe what's allowed in `march-config.xml`:

```xml
<rules>
    <configuration>
        <strategy>DEFAULT-DENY</strategy>
    </configuration>
    <rules>
        <rule>
            <description>Impl modules may depend on their own API module</description>
            <definition>
                source.domain == target.domain AND
                source.abstraction == abstraction.impl AND
                target.abstraction == abstraction.api
            </definition>
        </rule>
    </rules>
</rules>
```

Under `DEFAULT-DENY`, any dependency that isn't explicitly allowed fails the build —
whether it's a `<dependency>` in a `pom.xml` or an actual import in your code.

A march config (the XML file you point `<configFile>` at) has four parts, and they build on
each other in this order:

## Dimensions and partitions

A **dimension** is one axis you want to classify your code by. Examples are:

- `layer` (presentation/service/business/dbaccess)
- `domain` (order/article/user)
- `abstraction` (api/impl)

Each named value a dimension can take is called a **partition**.

```xml
<dimension>
    <name>layer</name>
    <partitions>
        <partition><name>presentation</name></partition>
        <partition><name>service</name></partition>
        <partition><name>business</name></partition>
        <partition><name>dbaccess</name></partition>
    </partitions>
</dimension>
```

Every module and package gets classified along some subset of these dimensions. A package
could be classified as `layer=service` and `domain=order` at the same time.

## Project structure: mapping dimensions onto real modules and packages

`<projectStructure>` describes the modularity of your architecture as a tree. It defines
which dimension is classified at each level of your project tree.

Each `<modularity>` node (for modules) or `<packageModularity>` node (for packages)
specifies:

- `dimension`: which dimension the components at this level must classify
- `case`: distinguishes between children, if a node has more than one

```xml
<modularity dimension="domain" groupId="com.example" artifactId="root">
    <modularity dimension="abstraction" artifactId="${domain}" groupId="com.example.${domain}">
        <modularity case="api" artifactId="${domain}-${abstraction}"
                    rootPackage="${groupId}.${abstraction}" allow="dto;presentation;service">
            <packageModularity dimension="layer" name="${layer}"/>
        </modularity>
        <modularity case="impl" artifactId="${domain}-${abstraction}"
                    rootPackage="${groupId}.${abstraction}">
            <packageModularity dimension="layer" name="${layer}"/>
        </modularity>
    </modularity>
</modularity>
```

- The root introduces `domain`, so every module has to classify a domain.
- The second level introduces `abstraction`, so within a domain, modules split into an `api`
  module and an `impl` module. Modules on this level are classified by both `domain` and
  `abstraction`.
- The third level differs between the `api` and `impl` branches, distinguished by `case`. The
  node is a `<packageModularity>`, meaning classification happens on the top-level packages
  *inside* each api/impl module, not on further modules:
    - `api` modules may only contain `dto`, `presentation` or `service` packages (`allow`
      restricts which partitions of `layer` are legal there).
    - `impl` modules allow any partition of `layer`.

`groupId`/`artifactId`/`rootPackage` are naming conventions, with `${dimension}` placeholders
substituted by the actual classified partition. march cross-checks them against your real
`pom.xml` coordinates and package names, and fails the build on a mismatch.

## Classifying modules and packages

The concrete `<modules>` section classifies your real modules and packages. Each component
classifies the dimension introduced at its level in the `projectStructure` tree, and inherits
all classifications from its parent components.

For the `projectStructure` defined above, classifying the `order` domain looks like this:

```xml
<modules>
    <module artifactId="root">
        <module partition="order" artifactId="order" groupId="com.example.order">
            <module partition="api" artifactId="order-api" rootPackage="com.example.order.api">
                <packageTemplate name="domainApi"/>
            </module>
            <module partition="impl" artifactId="order-impl" rootPackage="com.example.order.impl">
                <packageTemplate name="domainImpl"/>
            </module>
        </module>
    </module>
</modules>
```

`order-impl` sets its own `partition="impl"` (matching the `abstraction` dimension that
`projectStructure` introduces at that level) and inherits `partition="order"` from its
parent — so its full classification is `domain.order` + `abstraction.impl`, exactly what
rules compare against with `source.domain` / `source.abstraction`.

The `<packageTemplate>` reference is where `layer` comes in. `<packageTemplates>` exist so
the same package layout can be reused across every module of the same shape, instead of
repeating the same structure inline for every domain's api/impl module:

```xml
<packageTemplates>
    <packageTemplate name="domainApi">
        <jpackage name="dto" partition="dto"/>
        <jpackage name="presentation" partition="presentation"/>
        <jpackage name="service" partition="service"/>
    </packageTemplate>

    <packageTemplate name="domainImpl">
        <jpackage name="presentation" partition="presentation"/>
        <jpackage name="service" partition="service"/>
        <jpackage name="business" partition="business"/>
        <jpackage name="dbaccess" partition="dbaccess" optional="true"/>
    </packageTemplate>
</packageTemplates>
```

`name` is the literal sub-package folder name, `partition` is the `layer` value it's
classified as, and `optional="true"` allows a package to be absent without failing
structural validation. The `service` package inside `order-impl` ends up classified as
`domain.order` + `abstraction.impl` + `layer.service` — inherited from the module, plus its
own layer.

## Rules and strategy

With every module and package classified, `<rules>` decides which dependencies are actually
allowed. Two strategies are available, set once per config:

- **`DEFAULT-DENY`**: every dependency is forbidden unless some rule explicitly matches it.
- **`DEFAULT-ALLOW`**: every dependency is allowed unless some rule explicitly matches it.

Each `<rule>` has a `<definition>`: a boolean expression evaluated for every candidate
dependency, where `source` is the dependent side and `target` is the thing being depended on.
You compare a side's dimension against a fixed partition, another side's dimension, or `NULL`
(meaning that dimension isn't classified for that side at all):

```xml
<rule>
    <description>Impl modules may depend on their own API module</description>
    <definition>
        source.domain == target.domain AND
        source.abstraction == abstraction.impl AND
        target.abstraction == abstraction.api
    </definition>
</rule>
```

Supported operators: `==`, `!=`, `IN <dimension>.(a|b|c)`, combined with `AND`, `OR`, `!`
(NOT) and parentheses (`AND` binds tighter than `OR`).

By default a rule is checked against **both** the Maven module graph and the compiled
bytecode. `<scope>module_only</scope>` or `<scope>package_only</scope>` restricts a rule to
just one of the two. This matters because some dimensions (like `layer`, which lives inside a
module) are only ever classified on packages, never on whole modules — a rule using such a
dimension needs `package_only`, plus a separate, coarser `module_only` rule (using only
dimensions modules actually have) to legalize the corresponding `pom.xml` dependency.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
