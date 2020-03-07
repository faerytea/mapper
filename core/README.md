# Mapper generator

It will generate parsers and serializes for your annotated POJOs at compile time. 
This is core module, and you also need real parser/serializer (e.g. jackson).

### annotations module

Contains annotations and interfaces, you need it at runtime.
There is no annotation with `@Retention(RUNTIME)` and we will not use reflection.

### generators-base module

Usually you need it only if you want to generate code for specific parser.
You do not need it in application.

### processor module

This module will go through you code to extract and bundle everything needed by generator.
You should plug it as annotation processor

## Usage

### Use already made generators

1. Find generator compatible with this processor
2. Add to dependency section
```groovy
    annotationProcessor 'your.found.fancy:processor:1.2.3'
    annotationProcessor 'com.gitlab.faerytea.mapper:processor:0.1.17'
```
and everything needed to it (like `com.fasterxml.jackson.core:jackson-core`)
3. Configure: 
```groovy
compileJava.options.compilerArgs += '-AmapperGeneratorName=MyFancyProcessor'
```

### Make your own generator for already made stream parser / serializer 

1. Create separate project.
2. Add
```groovy
    implementation 'com.gitlab.faerytea.mapper:generators-base:0.7.0'
    implementation 'com.gitlab.faerytea.mapper:annotations:0.9'
    implementation 'com.squareup:javapoet:1.11.1'
```
Yes, there is Java Poet. It is nice and pretty simple.
3. Extend one of following:
  - `SimpleJsonGenerator`
    <br />Very simple and good starting point
  - `SimpleGenerator`
    <br />Like next, but without boilerplate
  - `Generator`
    <br />Yes, processor really uses all these methods
4. Build & publish. Maven local should be enough.

Then go on as in previous section. 
Your `MyFancyProcessor` is fully qualified class name of your class.