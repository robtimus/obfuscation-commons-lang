# obfuscation-commons-lang

Provides extensions to [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/) for obfuscating objects.

Currently it has one extension: an obfuscating [ToStringStyle](https://commons.apache.org/proper/commons-lang/javadocs/api-release/org/apache/commons/lang3/builder/ToStringStyle.html). This can obfuscate fields when used with [ToStringBuilder](https://commons.apache.org/proper/commons-lang/javadocs/api-release/org/apache/commons/lang3/builder/ToStringBuilder.html) or similar classes.

To create an obfuscating `ToStringStyle`, use one of the factory methods of class 
[ObfuscatingToStringStyle](https://robtimus.github.io/obfuscation-commons-lang/apidocs/com/github/robtimus/obfuscation/commons/lang3/ObfuscatingToStringStyle.html) to create a builder, add fields, and build the result. For instance, using the default style:

    ToStringStyle style = ObfuscatingToStringStyle.defaultStyle()
            .withField("password", Obfuscator.fixedLength(3))
            .build();

## Available styles

Most of the styles available in Apache Commons Lang 3 are available, including the recursive and multi-line recursive style. The only style that has been omitted is the [JSON toString style](https://commons.apache.org/proper/commons-lang/javadocs/api-release/org/apache/commons/lang3/builder/ToStringStyle.html#JSON_STYLE).

## Immutability

Most of the styles available in Apache Commons Lang 3 are all immutable. The same cannot be said for the obfuscating styles, they are not immutable and not thread-safe. Reusing the same instance should not be done concurrently (reusing it in the same thread should be possible).

However, it is possible to create immutable _suppliers_ instead:

    Supplier<? extends ToStringStyle> styleSupplier = ObfuscatingToStringStyle.defaultStyle()
            .withField("password", Obfuscator.fixedLength(3))
            .supplier();

The difference between using `build()` and using `supplier()` is that `supplier()` is more light-weight if you need to create multiple styles with the same settings. Such a supplier can be used directly or using `ThreadLocal`. For instance, in a class:

    private static final Supplier<? extends ToStringStyle> TO_STRING_STYLE = ObfuscatingToStringStyle.defaultStyle()
            ...
            .supplier();
    
    ...
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE.get());
    }

## Serializability

Obfuscating `ToStringStyle` instances are serializable if the obfuscators they use are. This most often means that they are not serializable, even though most `ToStringStyle` implementations are.

## Extending ObfuscatingToStringStyle

See [examples](https://github.com/robtimus/obfuscation-commons-lang/tree/master/src/examples/java/com/github/robtimus/obfuscation/commons/lang3/extending) for some examples. These include both an example that simply reuses [ObfuscatingToStringStyle.Builder](https://robtimus.github.io/obfuscation-commons-lang/apidocs/com/github/robtimus/obfuscation/commons/lang3/ObfuscatingToStringStyle.Builder.html), and one that provides a builder with more properties.
