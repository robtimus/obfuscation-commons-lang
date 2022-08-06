module com.github.robtimus.obfuscation.commons.lang {
    requires transitive com.github.robtimus.obfuscation;
    requires transitive org.apache.commons.lang3;

    exports com.github.robtimus.obfuscation.commons.lang3;

    opens com.github.robtimus.obfuscation.commons.lang3 to org.apache.commons.lang3;
}
