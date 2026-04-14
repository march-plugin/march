package io.github.march_plugin.core.config.testutil;

import io.github.march_plugin.core.config.dimensions.model.Dimension;

@SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:MissingJavadocMethod"})
public final class TestUtil {

    public final Dimension layerDimension;
    public final Dimension.Partition presentationPartition;
    public final Dimension.Partition servicePartition;
    public final Dimension.Partition businessPartition;
    public final Dimension.Partition dbAccessPartition;

    public final Dimension domainDimension;
    public final Dimension.Partition userPartition;
    public final Dimension.Partition articlePartition;
    public final Dimension.Partition orderPartition;

    public TestUtil() {
        final var layerDimensionBuilder = new Dimension.Builder("layer");
        presentationPartition = layerDimensionBuilder.addPartition("presentation");
        servicePartition = layerDimensionBuilder.addPartition("service");
        businessPartition = layerDimensionBuilder.addPartition("business");
        dbAccessPartition = layerDimensionBuilder.addPartition("db-access");
        layerDimension = layerDimensionBuilder.build();

        final var domainDimensionBuilder = new Dimension.Builder("domain");
        userPartition = domainDimensionBuilder.addPartition("user");
        articlePartition = domainDimensionBuilder.addPartition("article");
        orderPartition = domainDimensionBuilder.addPartition("order");
        domainDimension = domainDimensionBuilder.build();
    }
}
