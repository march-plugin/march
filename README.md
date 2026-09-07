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
<march>
    <settings>
        <ruleEngine>
            <ruleStrategy>DEFAULT-DENY</ruleStrategy>
        </ruleEngine>
    </settings>
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
</march>
```

Under `DEFAULT-DENY`, any unallowed dependency fails the build. This covers a `<dependency>`
in a `pom.xml` and an actual import in your code.

Use this xsd for IDE-support when writing March Config file.

```xml
<march xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/march-plugin/march/main/march-maven-plugin/src/main/resources/march-config.xsd">
  <!-- ...  -->
</march>
```

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
parent. Its full classification is `domain.order` + `abstraction.impl`. That's exactly what
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
`domain.order` + `abstraction.impl` + `layer.service`. It inherits domain and abstraction
from the module, and adds its own layer.

### Classifying external dependencies with virtual modules

Rules must also govern third-party dependencies, like a logging library, even though march
never sees their source. `virtualModule` and `virtualModuleRef` classify external
`groupId:artifactId` coordinates as if they were your own modules. The same rules then cover
them:

```xml
<module artifactId="util" partition="util">
    <virtualModule partition="logging" virtualGroupId="com.example.util.logging" virtualArtifactId="logging">
        <virtualModuleRef partition="api" groupId="org.slf4j" artifactId="slf4j-api"
                           virtualGroupId="com.example.util.logging" virtualArtifactId="logging-api"/>
        <virtualModuleRef partition="impl" groupId="org.apache.logging.log4j" artifactId="log4j-slf4j2-impl"
                           virtualGroupId="com.example.util.logging" virtualArtifactId="logging-impl"/>
    </virtualModule>
</module>
```

`virtualModule` groups related external dependencies under one classification. Here that's
`util.logging`, split further by `package_abstraction`. Each `virtualModuleRef` maps one real
`groupId:artifactId` onto a `virtualGroupId:virtualArtifactId` pair. That pair is the identity
march uses in its classification tree and in rule violation messages. `virtualModule` can nest
inside another `virtualModule` to classify along more than one dimension, the same way real
modules do.

This isn't optional. **Every `<dependency>` in the project, including test-scoped ones, must
resolve to a real classified module or a `virtualModuleRef`.** An unclassified dependency fails
the build. Test frameworks need a `virtualModule`/`virtualModuleRef` too.

## Rules and strategy

With every module and package classified, `<rules>` decides which dependencies are actually
allowed. Two strategies are available, set once per config:

- **`DEFAULT-DENY`**: every dependency is forbidden unless some rule explicitly matches it.
- **`DEFAULT-ALLOW`**: every dependency is allowed unless some rule explicitly matches it.

By default a rule is checked against **both** the Maven module graph and the compiled bytecode.
`<scope>module_only</scope>` or `<scope>package_only</scope>` restricts a rule to just one of the two.

`<scopeStrategy>` controls if package rules affect module scope:
- **`MANUAL`** (default): If a cross-module package dependency is needed, users must manually define a rule allowing the module dependency, mostly with `module_only` scope.
- **`AUTOMATIC`**: If any cross-module package dependency is allowed, then the module dependency is automatically allowed too.

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

## Static dependency-declaration checks

march also enforces a few Maven dependency-hygiene checks, independent of `<rules>`. They run
on every `<dependency>` and `dependencyManagement` entry, on every build. `<staticEnforcement>`
(inside `<settings>`) turns individual checks off. Omit it entirely to keep the defaults:

```xml
<settings>
    <staticEnforcement>
        <requireManagedVersion>true</requireManagedVersion>
        <forbidInlineVersion>true</forbidInlineVersion>
        <forbidInlineScope>true</forbidInlineScope>
        <forbidExclusions>false</forbidExclusions>
        <requireVersionProperty>true</requireVersionProperty>
    </staticEnforcement>
</settings>
```

| Check | Default | Rejects |
|---|---|---|
| `requireManagedVersion` | `true` | A `dependencyManagement` entry with no `<version>`. |
| `forbidInlineVersion` | `true` | A `<dependency>` declaring its own `<version>` instead of relying on `dependencyManagement`. |
| `forbidInlineScope` | `true` | A `<dependency>` declaring its own `<scope>` instead of relying on `dependencyManagement`. |
| `forbidExclusions` | `false` | A `<dependency>` declaring any `<exclusions>` at all. |
| `requireVersionProperty` | `true` | A version (inline or in `dependencyManagement`) that isn't a `${property}` reference. |

## Inspecting your configuration

This chapter shows how to use command line tools to analyze your march configuration.

### `march:tree`

Prints the classification tree of all modules and packages from `dimensions`, `projectStructure`,
`packageTemplates` and `modules`.
Use `-Dmarch.showInherited=true` to include inherited classifications.

```
mvn march:tree
mvn march:tree -Dmarch.showInherited=true
```

### `march:matrix`

Prints a package-level permission matrix across an abstract set of dimension combinations: for
every pair of classifications, whether a dependency between them is `Allowed`, `Forbidden`, or
`PartiallyAllowed` (with the residual rule condition shown), using the same three-valued
rule reduction used at build time for package (bytecode) dependencies. This is useful for
spotting a rule that reaches further, or less far, than intended.

```
mvn march:matrix
mvn march:matrix -Dclassifications="{domain;layer}"
mvn march:matrix -Dclassifications="{domain(article;order);layer(api;impl)}" -Dmarch.columnWidth=10
```

- `-Dclassifications`: which dimensions (and optionally which of their partitions) to include,
  e.g. `{domain(article;order);layer}`. Defaults to every configured dimension with all of its
  partitions.
- `-Dmarch.columnWidth`: characters shown per column before a label truncates (default `5`).
  Raise it if truncated partition names collide into the same column, e.g. `adapterIn` and
  `adapterOut` both showing as `adapt`.

A rule's `<scope>` (see [Rules and strategy](#rules-and-strategy)) matters here too: `module_only`
rules are excluded, since the matrix only ever evaluates package-level (bytecode) dependencies.

### `march:module-matrix`

Prints a module-level dependency permission matrix across every real, fully classified module.
It's the module-level counterpart to `march:matrix`'s package-level, abstract view. A letter
marks the deciding rule. Under `DEFAULT-DENY` it's the rule that *allows* the dependency (blank
means forbidden by default). Under `DEFAULT-ALLOW` it's the rule that *forbids* it (`OK` means
allowed by default). With `DEFAULT-DENY` and `AUTOMATIC` scope strategy, a lowercase letter means
the dependency is allowed only via the package-level fallback, with no direct module-level rule.

```
mvn march:module-matrix
mvn march:module-matrix -Dmarch.columnWidth=6
mvn march:module-matrix -Dmarch.showRules=false
```

- `-Dmarch.columnWidth`: characters shown per column before a label truncates (default `4`).
- `-Dmarch.showRules`: whether cells show which rule matched (default `true`). Set to `false` to
  collapse cells to just `OK` (allowed) or blank (forbidden), without revealing which specific
  rule decided that.

## License

Apache License 2.0. See [LICENSE](LICENSE).
