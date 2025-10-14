# JSpecify Maven Plugin

Generate package-level `@NullMarked` annotations for your project automatically. This Maven plugin scans your source packages and creates minimal `package-info.java` files annotated with `@org.jspecify.annotations.NullMarked` in a generated-sources folder, then adds that folder to the compilation roots.

## Why?
Annotating every package with JSpecify’s `@NullMarked` makes non-null the default for your codebase. Doing this by hand can be tedious and error-prone. This plugin:
- Finds all packages that contain Java sources
- Skips packages that already define a `package-info.java`
- Writes generated `package-info.java` files under `target/generated-sources/jspecify`
- Registers that directory as a compile source root so the files participate in compilation

Your original sources remain untouched.

## Requirements
- Java 21 (the plugin is compiled for Java 21 and Maven must run on JDK 21)
- Maven 3.3.9 or newer

## Quick start
Add the plugin to your project’s `pom.xml` and (optionally) bind it explicitly:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.fuchss</groupId>
      <artifactId>jspecify-maven-plugin</artifactId>
      <version>${VERSION}</version>
      <executions>
        <execution>
          <goals>
            <goal>generate</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
  
  <!-- Ensure the generated sources are compiled (the plugin already adds the source root) -->
  <!-- No additional configuration normally required -->
</build>
```

Also make sure the JSpecify annotations are on your compile classpath (the generated `package-info.java` references them):

```xml
<dependencies>
  <dependency>
    <groupId>org.jspecify</groupId>
    <artifactId>jspecify</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

Now run your build; during `generate-sources` the plugin will create the missing `package-info.java` files:

```sh
mvn generate-sources
```

## What gets generated
For each package under your source directory that contains at least one `.java` file and does not already have a `package-info.java`, the plugin writes:

```java
@org.jspecify.annotations.NullMarked
package your.package.name;
```

Files are created under `target/generated-sources/jspecify/` mirroring your package structure. On subsequent runs, the plugin cleans previously generated files in that folder before regenerating.

Packages that already define a `package-info.java` are never overwritten.

## Configuration
The goal accepts two parameters. All parameters are optional; defaults are shown below.

- sourceDir: Directory to scan for Java packages.
  - Default: `${project.basedir}/src/main/java`
- generatedSourcesDirectory: Output directory for generated `package-info.java` files.
  - Default: `${project.build.directory}/generated-sources/jspecify`


## Troubleshooting
- Compilation error “package org.jspecify.annotations does not exist”
  - Add the JSpecify annotations dependency to your project (see Quick start). The plugin generates fully qualified `@org.jspecify.annotations.NullMarked`, but your build still needs the annotation jar on the compile classpath.
- Nothing is generated
  - The plugin only generates files for directories that contain at least one `.java` file. It also skips packages that already have a `package-info.java`.
  - Check that `sourceDir` points at the correct sources (default is `src/main/java`).

## License
This project is licensed under the MIT License. See [`LICENSE`](LICENSE).
