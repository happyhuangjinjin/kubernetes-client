/*
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.java.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import io.fabric8.java.generator.exceptions.JavaGeneratorException;
import io.fabric8.java.generator.nodes.*;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsOrBool;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static io.fabric8.java.generator.CRGeneratorRunner.groupToPackage;
import static io.fabric8.java.generator.nodes.Keywords.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {

  static final Config defaultConfig = new Config();

  @Test
  void testCorrectInterpolationOfPackage() {
    // Act
    String packageName = groupToPackage("test.org");

    // Assert
    assertEquals("org.test", packageName);
  }

  @Test
  void testCR() {
    // Arrange
    JCRObject cro = new JCRObject(
        "v1alpha1",
        "Type",
        "g",
        "v",
        "Namespaced",
        Collections.emptyMap(),
        Collections.emptyList(),
        false,
        "",
        true,
        true,
        "t",
        "ts",
        defaultConfig);

    // Act
    GeneratorResult res = cro.generateJava();

    // Assert
    assertThat(res.getTopLevelClasses())
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "Type")
        .extracting(GeneratorResult.ClassResult::getPackageDeclaration)
        .asInstanceOf(InstanceOfAssertFactories.optional(PackageDeclaration.class))
        .get()
        .hasFieldOrPropertyWithValue("nameAsString", "v1alpha1");
  }

  @Test
  void testNamespacedCR() {
    // Arrange
    JCRObject cro = new JCRObject(
        null,
        "Type",
        "g",
        "v",
        "Namespaced",
        Collections.emptyMap(),
        Collections.emptyList(),
        false,
        "",
        true,
        true,
        "t",
        "ts",
        defaultConfig);

    // Act
    GeneratorResult res = cro.generateJava();

    // Assert
    assertEquals("io.fabric8.kubernetes.api.model.Namespaced", res.getTopLevelClasses().get(0)
        .getClassByName("Type").get().getImplementedTypes().get(0).getNameWithScope());
  }

  @Test
  void testClusterScopeCR() {
    // Arrange
    JCRObject cro = new JCRObject(
        null,
        "Type",
        "g",
        "v",
        "Cluster",
        Collections.emptyMap(),
        Collections.emptyList(),
        false,
        "",
        true,
        true,
        "t",
        "ts",
        defaultConfig);

    // Act
    GeneratorResult res = cro.generateJava();

    // Assert
    assertTrue(res.getTopLevelClasses().get(0).getClassByName("Type").get().getImplementedTypes().isEmpty());
  }

  @Test
  void testCRWithoutNamespace() {
    // Arrange
    JCRObject cro = new JCRObject(
        null,
        "Type",
        "g",
        "v",
        "Namespaced",
        Collections.emptyMap(),
        Collections.emptyList(),
        false,
        "",
        true,
        true,
        "t",
        "ts",
        defaultConfig);

    // Act
    GeneratorResult res = cro.generateJava();

    // Assert
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("Type", res.getTopLevelClasses().get(0).getName());
  }

  @Test
  void testPrimitive() {
    // Arrange
    JPrimitive primitive = new JPrimitive(
        "test",
        defaultConfig,
        null,
        Boolean.FALSE,
        null,
        null);

    // Act
    GeneratorResult res = primitive.generateJava();

    // Assert
    assertEquals("test", primitive.getType());
    assertEquals(0, res.getTopLevelClasses().size());
  }

  @Test
  void testPrimitiveWithNumericValidationConstraints() {
    // Arrange
    final Double expectedMaximum = 3.14;
    final Double expectedMinimum = 0.0;
    ValidationProperties validationProperties = ValidationProperties.Builder
        .getInstance()
        .withMaximum(expectedMaximum)
        .withMinimum(expectedMinimum)
        .build();
    JPrimitive primitive = new JPrimitive(
        "test",
        defaultConfig,
        null,
        Boolean.FALSE,
        null,
        validationProperties);

    // Act
    GeneratorResult res = primitive.generateJava();

    // Assert
    assertEquals("test", primitive.getType());
    assertEquals(0, res.getTopLevelClasses().size());
    assertEquals(expectedMaximum, primitive.getMaximum());
    assertEquals(expectedMinimum, primitive.getMinimum());
    assertNull(primitive.getPattern());
  }

  @Test
  void testPrimitiveWithAlphanumericValidationConstraints() {
    // Arrange
    final String expectedPattern = ".*whatever.*";
    ValidationProperties validationProperties = ValidationProperties.Builder
        .getInstance()
        .withPattern(expectedPattern)
        .build();
    JPrimitive primitive = new JPrimitive(
        "test",
        defaultConfig,
        null,
        Boolean.FALSE,
        null,
        validationProperties);

    // Act
    GeneratorResult res = primitive.generateJava();

    // Assert
    assertEquals("test", primitive.getType());
    assertEquals(0, res.getTopLevelClasses().size());
    assertEquals(expectedPattern, primitive.getPattern());
    assertNull(primitive.getMaximum());
    assertNull(primitive.getMinimum());
  }

  @Test
  void testArrayOfPrimitives() {
    // Arrange
    JArray array = new JArray(
        new JPrimitive(
            "primitive",
            defaultConfig,
            null,
            Boolean.FALSE,
            null,
            null),
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = array.generateJava();

    // Assert
    assertEquals("java.util.List<primitive>", array.getType());
    assertEquals(0, res.getTopLevelClasses().size());
  }

  @Test
  void testMapOfPrimitives() {
    // Arrange
    JMap map = new JMap(
        new JPrimitive(
            "primitive",
            defaultConfig,
            null,
            Boolean.FALSE,
            null,
            null),
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = map.generateJava();

    // Assert
    assertEquals("java.util.Map<java.lang.String, primitive>", map.getType());
    assertEquals(0, res.getTopLevelClasses().size());
  }

  @Test
  void testEmptyObject() {
    // Arrange
    JObject obj = new JObject(
        "v1alpha1",
        "t",
        null,
        null,
        false,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals("v1alpha1.T", obj.getType());
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("T", res.getTopLevelClasses().get(0).getName());
  }

  @Test
  void testEmptyObjectWithoutNamespace() {
    // Arrange
    JObject obj = new JObject(
        null,
        "t",
        null,
        null,
        false,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals("T", obj.getType());
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("T", res.getTopLevelClasses().get(0).getName());
  }

  @Test
  void testObjectOfPrimitives() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps newBool = new JSONSchemaProps();
    newBool.setType("boolean");
    props.put("o1", newBool);
    JObject obj = new JObject(
        "v1alpha1",
        "t",
        props,
        null,
        false,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals("v1alpha1.T", obj.getType());
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("T", res.getTopLevelClasses().get(0).getName());

    Optional<ClassOrInterfaceDeclaration> clz = res.getTopLevelClasses().get(0).getClassByName("T");
    assertTrue(clz.isPresent());
    assertEquals(1, clz.get().getFields().size());
    assertTrue(clz.get().getFieldByName("o1").isPresent());
  }

  @Test
  void testObjectWithRequiredField() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps newBool = new JSONSchemaProps();
    newBool.setType("boolean");
    props.put("o1", newBool);
    List<String> req = new ArrayList<>(1);
    req.add("o1");
    JObject obj = new JObject(
        "v1alpha1",
        "t",
        props,
        req,
        false,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    Optional<ClassOrInterfaceDeclaration> clz = res.getTopLevelClasses().get(0).getClassByName("T");
    assertTrue(clz.get().getFieldByName("o1").get().getAnnotationByName("Required").isPresent());
  }

  @Test
  void testObjectWithAndWithoutGeneratedAnnotation() {
    // Arrange
    JObject obj1 = new JObject(
        "v1alpha1",
        "t",
        new HashMap<>(),
        new ArrayList<>(),
        false,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);
    Config config = new Config(null, null, false, new HashMap<>());
    JObject obj2 = new JObject(
        "v1alpha1",
        "t",
        new HashMap<>(),
        new ArrayList<>(),
        false,
        config,
        null,
        Boolean.FALSE,
        null);
    String generatedAnnotationName = AbstractJSONSchema2Pojo.newGeneratedAnnotation().getNameAsString();

    // Act
    GeneratorResult res1 = obj1.generateJava();
    GeneratorResult res2 = obj2.generateJava();

    // Assert
    Optional<ClassOrInterfaceDeclaration> clz1 = res1.getTopLevelClasses().get(0).getClassByName("T");
    assertTrue(clz1.get().getAnnotationByName(generatedAnnotationName).isPresent());

    Optional<ClassOrInterfaceDeclaration> clz2 = res2.getTopLevelClasses().get(0).getClassByName("T");
    assertFalse(clz2.get().getAnnotationByName(generatedAnnotationName).isPresent());
  }

  @Test
  void testDefaultEnum() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps newEnum = new JSONSchemaProps();
    newEnum.setType("string");
    List<JsonNode> enumValues = new ArrayList<>();
    enumValues.add(new TextNode("foo"));
    enumValues.add(new TextNode("bar"));
    enumValues.add(new TextNode("baz"));
    props.put("e1", newEnum);
    JEnum enu = new JEnum(
        "t",
        JAVA_LANG_STRING,
        enumValues,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = enu.generateJava();

    // Assert
    assertEquals("T", enu.getType());
    assertEquals(1, res.getInnerClasses().size());
    assertEquals("T", res.getInnerClasses().get(0).getName());

    Optional<EnumDeclaration> en = res.getInnerClasses().get(0).getEnumByName("T");
    assertTrue(en.isPresent());
    assertEquals(3, en.get().getEntries().size());
    assertEquals("FOO", en.get().getEntries().get(0).getName().asString());
    assertEquals("BAR", en.get().getEntries().get(1).getName().asString());
    assertEquals("BAZ", en.get().getEntries().get(2).getName().asString());
  }

  @Test
  void testLongEnum() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps newEnum = new JSONSchemaProps();
    newEnum.setType("integer");
    List<JsonNode> enumValues = new ArrayList<>();
    enumValues.add(new TextNode("1"));
    enumValues.add(new TextNode("2"));
    enumValues.add(new TextNode("3"));
    props.put("e1", newEnum);
    JEnum enu = new JEnum(
        "t",
        JAVA_LANG_LONG,
        enumValues,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = enu.generateJava();

    // Assert
    assertEquals("T", enu.getType());
    assertEquals(1, res.getInnerClasses().size());
    assertEquals("T", res.getInnerClasses().get(0).getName());

    Optional<EnumDeclaration> en = res.getInnerClasses().get(0).getEnumByName("T");
    assertTrue(en.isPresent());
    assertEquals(3, en.get().getEntries().size());
    assertEquals("V__1", en.get().getEntries().get(0).getName().asString());
    assertEquals("V__2", en.get().getEntries().get(1).getName().asString());
    assertEquals("V__3", en.get().getEntries().get(2).getName().asString());
    assertEquals("1L", en.get().getEntries().get(0).getArgument(0).toString());
    assertEquals("2L", en.get().getEntries().get(1).getArgument(0).toString());
    assertEquals("3L", en.get().getEntries().get(2).getArgument(0).toString());
  }

  @Test
  void testIntEnum() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps newEnum = new JSONSchemaProps();
    newEnum.setType("integer");
    newEnum.setFormat("int32");
    List<JsonNode> enumValues = new ArrayList<>();
    enumValues.add(new TextNode("1"));
    enumValues.add(new TextNode("2"));
    enumValues.add(new TextNode("3"));
    props.put("e1", newEnum);
    JEnum enu = new JEnum(
        "t",
        JAVA_LANG_INTEGER,
        enumValues,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = enu.generateJava();

    // Assert
    assertEquals("T", enu.getType());
    assertEquals(1, res.getInnerClasses().size());
    assertEquals("T", res.getInnerClasses().get(0).getName());

    Optional<EnumDeclaration> en = res.getInnerClasses().get(0).getEnumByName("T");
    assertTrue(en.isPresent());
    assertEquals(3, en.get().getEntries().size());
    assertEquals("V__1", en.get().getEntries().get(0).getName().asString());
    assertEquals("V__2", en.get().getEntries().get(1).getName().asString());
    assertEquals("V__3", en.get().getEntries().get(2).getName().asString());
    assertEquals("1", en.get().getEntries().get(0).getArgument(0).toString());
    assertEquals("2", en.get().getEntries().get(1).getArgument(0).toString());
    assertEquals("3", en.get().getEntries().get(2).getArgument(0).toString());
  }

  @Test
  void testBooleanEnum() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps newEnum = new JSONSchemaProps();
    newEnum.setType("boolean");
    List<JsonNode> enumValues = new ArrayList<>();
    enumValues.add(new TextNode("true"));
    enumValues.add(new TextNode("false"));
    props.put("e1", newEnum);
    JEnum enu = new JEnum(
        "t",
        JAVA_PRIMITIVE_BOOLEAN,
        enumValues,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = enu.generateJava();

    // Assert
    assertEquals("T", enu.getType());
    assertEquals(1, res.getInnerClasses().size());
    assertEquals("T", res.getInnerClasses().get(0).getName());

    Optional<EnumDeclaration> en = res.getInnerClasses().get(0).getEnumByName("T");
    assertTrue(en.isPresent());
    assertEquals(2, en.get().getEntries().size());
    assertEquals("TRUE", en.get().getEntries().get(0).getName().asString());
    assertEquals("FALSE", en.get().getEntries().get(1).getName().asString());
    assertEquals("true", en.get().getEntries().get(0).getArgument(0).toString());
    assertEquals("false", en.get().getEntries().get(1).getArgument(0).toString());
  }

  @Test
  void testNotUppercaseEnum() {
    // Arrange
    CompilationUnit cu = new CompilationUnit();
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps newEnum = new JSONSchemaProps();
    newEnum.setType("string");
    List<JsonNode> enumValues = new ArrayList<>();
    enumValues.add(new TextNode("foo"));
    enumValues.add(new TextNode("bar"));
    enumValues.add(new TextNode("baz"));
    props.put("e1", newEnum);
    JEnum enu = new JEnum(
        "t",
        JAVA_LANG_STRING,
        enumValues,
        new Config(false, null, null, new HashMap<>()),
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = enu.generateJava();

    // Assert
    assertEquals("T", enu.getType());
    assertEquals(1, res.getInnerClasses().size());
    assertEquals("T", res.getInnerClasses().get(0).getName());

    Optional<EnumDeclaration> en = res.getInnerClasses().get(0).getEnumByName("T");
    assertTrue(en.isPresent());
    assertEquals(3, en.get().getEntries().size());
    assertEquals("foo", en.get().getEntries().get(0).getName().asString());
    assertEquals("bar", en.get().getEntries().get(1).getName().asString());
    assertEquals("baz", en.get().getEntries().get(2).getName().asString());
  }

  @Test
  void testArrayOfObjects() {
    // Arrange
    JArray array = new JArray(
        new JObject(
            null,
            "t",
            null,
            null,
            false,
            defaultConfig,
            null,
            Boolean.FALSE,
            null),
        defaultConfig,
        null,
        false,
        null);

    // Act
    GeneratorResult res = array.generateJava();

    // Assert
    assertEquals("java.util.List<T>", array.getType());
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("T", res.getTopLevelClasses().get(0).getName());
  }

  @Test
  void testMapOfObjects() {
    // Arrange
    JMap map = new JMap(
        new JObject(
            null,
            "t",
            null,
            null,
            false,
            defaultConfig,
            null,
            Boolean.FALSE,
            null),
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = map.generateJava();

    // Assert
    assertEquals("java.util.Map<java.lang.String, T>", map.getType());
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("T", res.getTopLevelClasses().get(0).getName());
  }

  @Test
  void testObjectOfObjects() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps newObj = new JSONSchemaProps();
    newObj.setType("object");
    props.put("o1", newObj);
    JObject obj = new JObject(
        null,
        "t",
        props,
        null,
        false,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals(2, res.getTopLevelClasses().size());
    assertEquals("O1", res.getTopLevelClasses().get(0).getName());
    assertEquals("T", res.getTopLevelClasses().get(1).getName());

    Optional<ClassOrInterfaceDeclaration> clzT = res.getTopLevelClasses().get(1).getClassByName("T");
    assertTrue(clzT.isPresent());
    assertEquals(1, clzT.get().getFields().size());
    assertTrue(clzT.get().getFieldByName("o1").isPresent());
    Optional<ClassOrInterfaceDeclaration> clzO1 = res.getTopLevelClasses().get(0).getClassByName("O1");
    assertTrue(clzO1.isPresent());
  }

  @Test
  void testObjectWithPreservedFields() {
    // Arrange
    JObject obj = new JObject(
        null,
        "t",
        null,
        null,
        true,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("T", res.getTopLevelClasses().get(0).getName());

    Optional<ClassOrInterfaceDeclaration> clzT = res.getTopLevelClasses().get(0).getClassByName("T");
    assertTrue(clzT.isPresent());
    assertTrue(clzT.get().getFieldByName("additionalProperties").isPresent());
  }

  @Test
  void testConfigToGeneratePreservedUnknownFields() {
    // Arrange
    Config config = new Config(null, null, null, true, new HashMap<>(), new ArrayList<>(), null, null, null);
    JObject obj = new JObject(
        null,
        "t",
        null,
        null,
        false,
        config,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("T", res.getTopLevelClasses().get(0).getName());

    Optional<ClassOrInterfaceDeclaration> clzT = res.getTopLevelClasses().get(0).getClassByName("T");
    assertTrue(clzT.isPresent());
    assertTrue(clzT.get().getFieldByName("additionalProperties").isPresent());
  }

  @Test
  void testObjectWithSpecialFieldNames() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps newObj = new JSONSchemaProps();
    newObj.setType("string");
    props.put("description", newObj);

    JObject obj = new JObject(
        null,
        "t",
        props,
        null,
        false,
        defaultConfig,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("T", res.getTopLevelClasses().get(0).getName());

    Optional<ClassOrInterfaceDeclaration> clzT = res.getTopLevelClasses().get(0).getClassByName("T");
    assertTrue(clzT.isPresent());
    assertTrue(clzT.get().getFieldByName("description").isPresent());
  }

  @Test
  void testObjectNullableFieldsManagement() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps nullableObj = new JSONSchemaProps();
    nullableObj.setType("object");
    nullableObj.setNullable(Boolean.TRUE);
    props.put("o1", nullableObj);

    JSONSchemaProps nonNullableObj = new JSONSchemaProps();
    nonNullableObj.setType("object");
    nonNullableObj.setNullable(Boolean.FALSE);
    props.put("o2", nonNullableObj);

    JObject obj = new JObject(null, "t", props, null, false, defaultConfig, null, Boolean.FALSE, null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals(3, res.getTopLevelClasses().size());
    assertEquals("O1", res.getTopLevelClasses().get(0).getName());
    assertEquals("O2", res.getTopLevelClasses().get(1).getName());
    assertEquals("T", res.getTopLevelClasses().get(2).getName());

    Optional<ClassOrInterfaceDeclaration> clzT = res.getTopLevelClasses().get(2).getClassByName("T");
    assertTrue(clzT.isPresent());
    assertEquals(2, clzT.get().getFields().size());
    Optional<FieldDeclaration> o1Field = clzT.get().getFieldByName("o1");
    assertTrue(o1Field.isPresent());
    FieldDeclaration actualO1Field = o1Field.get();
    Optional<AnnotationExpr> nullableJacksonBasedAnnotation = actualO1Field
        .getAnnotationByName("com.fasterxml.jackson.annotation.JsonSetter");
    assertTrue(nullableJacksonBasedAnnotation.isPresent());
    assertInstanceOf(SingleMemberAnnotationExpr.class, nullableJacksonBasedAnnotation.get());
    SingleMemberAnnotationExpr actualNullableAnnotation = (SingleMemberAnnotationExpr) nullableJacksonBasedAnnotation.get();
    assertEquals("nulls = com.fasterxml.jackson.annotation.Nulls.SET", actualNullableAnnotation.getMemberValue().toString());
    Optional<AnnotationExpr> nullableFabric8BasedAnnotation = actualO1Field
        .getAnnotationByName("io.fabric8.generator.annotation.Nullable");
    assertTrue(nullableFabric8BasedAnnotation.isPresent());

    Optional<FieldDeclaration> o2Field = clzT.get().getFieldByName("o2");
    assertTrue(o2Field.isPresent());
    FieldDeclaration actualO2Field = o2Field.get();
    nullableJacksonBasedAnnotation = actualO2Field.getAnnotationByName("com.fasterxml.jackson.annotation.JsonSetter");
    assertTrue(nullableJacksonBasedAnnotation.isPresent());
    assertInstanceOf(SingleMemberAnnotationExpr.class, nullableJacksonBasedAnnotation.get());
    actualNullableAnnotation = (SingleMemberAnnotationExpr) nullableJacksonBasedAnnotation.get();
    assertEquals("nulls = com.fasterxml.jackson.annotation.Nulls.SKIP", actualNullableAnnotation.getMemberValue().toString());

    Optional<ClassOrInterfaceDeclaration> clzO1 = res.getTopLevelClasses().get(0).getClassByName("O1");
    assertTrue(clzO1.isPresent());
  }

  @Test
  void testObjectWithAdditionalPropertiesTrue() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();
    JSONSchemaProps obj = new JSONSchemaProps();
    obj.setType("object");
    obj.setAdditionalProperties(new JSONSchemaPropsOrBool(true, null));
    props.put("o1", obj);

    JObject jobj = new JObject(null, "t", props, null, false, defaultConfig, null, Boolean.FALSE, null);

    // Act
    GeneratorResult res = jobj.generateJava();

    // Assert
    assertEquals(1, res.getTopLevelClasses().size());
    assertEquals("T", res.getTopLevelClasses().get(0).getName());

    Optional<ClassOrInterfaceDeclaration> clzT = res.getTopLevelClasses().get(0).getClassByName("T");
    assertTrue(clzT.isPresent());
    assertEquals(1, clzT.get().getFields().size());
    Optional<FieldDeclaration> o1Field = clzT.get().getFieldByName("o1");
    assertTrue(o1Field.isPresent());
    FieldDeclaration actualO1Field = o1Field.get();
    assertEquals("io.fabric8.kubernetes.api.model.AnyType",
        actualO1Field.getElementType().asString());
  }

  @Nested
  class ClassNameDisambiguationWithPackageNesting {

    private GeneratorResult result;

    @BeforeEach
    void classWithNestedClassesAndProperties() {
      Map<String, JSONSchemaProps> props = new HashMap<>();
      JSONSchemaProps newObj1 = new JSONSchemaProps();
      JSONSchemaProps newObj2 = new JSONSchemaProps();
      newObj1.setType("object");
      newObj2.setType("object");
      Map<String, JSONSchemaProps> obj2Props = new HashMap<>();
      obj2Props.put("o1", newObj1);
      obj2Props.put("o2", newObj1);
      obj2Props.put("o3", newObj1);
      newObj2.setProperties(obj2Props);
      props.put("o1", newObj1);
      props.put("o2", newObj2);
      result = new JObject("v1alpha1", "t", props, null, false, defaultConfig, null, Boolean.FALSE, null)
          .generateJava();
    }

    @Test
    void generatesMultipleClassesInNestedPackages() {
      assertThat(result.getTopLevelClasses())
          .hasSize(6)
          .extracting(
              // Class Name
              GeneratorResult.ClassResult::getName,
              // Package
              cr -> cr.getPackageDeclaration().map(PackageDeclaration::getName).map(Objects::toString).orElse(null))
          // The order here is not important
          .containsExactly(
              tuple("O1", "v1alpha1.t"),
              tuple("O1", "v1alpha1.t.o2"),
              tuple("O2", "v1alpha1.t.o2"),
              tuple("O3", "v1alpha1.t.o2"),
              tuple("O2", "v1alpha1.t"),
              tuple("T", "v1alpha1"));
    }

    @Test
    void generatesMainClassWithFields() {
      assertThat(result.getTopLevelClasses())
          .last()
          .hasFieldOrPropertyWithValue("name", "T")
          .extracting(c -> c.getClassByName("T"))
          .asInstanceOf(InstanceOfAssertFactories.optional(ClassOrInterfaceDeclaration.class)).get()
          .extracting(ClassOrInterfaceDeclaration::getFields)
          .asInstanceOf(InstanceOfAssertFactories.list(FieldDeclaration.class))
          .extracting(
              fd -> fd.getElementType().asString(),
              fd -> fd.getVariable(0).getName().asString())
          .containsExactly(
              tuple("v1alpha1.t.O1", "o1"),
              tuple("v1alpha1.t.O2", "o2"));
    }

    @Test
    void generatesNestedClass1WithEmptyFields() {
      assertThat(result.getTopLevelClasses())
          .first()
          .hasFieldOrPropertyWithValue("name", "O1")
          .extracting(c -> c.getClassByName("O1"))
          .asInstanceOf(InstanceOfAssertFactories.optional(ClassOrInterfaceDeclaration.class)).get()
          .extracting(ClassOrInterfaceDeclaration::getFields)
          .asInstanceOf(InstanceOfAssertFactories.list(FieldDeclaration.class))
          .isEmpty();
    }

    @Test
    void generatesNestedClass2WithFields() {
      assertThat(result.getTopLevelClasses())
          .element(4)
          .hasFieldOrPropertyWithValue("name", "O2")
          .extracting(c -> c.getClassByName("O2"))
          .asInstanceOf(InstanceOfAssertFactories.optional(ClassOrInterfaceDeclaration.class)).get()
          .extracting(ClassOrInterfaceDeclaration::getFields)
          .asInstanceOf(InstanceOfAssertFactories.list(FieldDeclaration.class))
          .extracting(
              fd -> fd.getElementType().asString(),
              fd -> fd.getVariable(0).getName().asString())
          .containsExactly(
              tuple("v1alpha1.t.o2.O1", "o1"),
              tuple("v1alpha1.t.o2.O2", "o2"),
              tuple("v1alpha1.t.o2.O3", "o3"));
    }
  }

  @Test
  void testObjectDefaultFieldsManagement() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();

    JSONSchemaProps objWithDefaultValues = new JSONSchemaProps();
    objWithDefaultValues.setType("object");
    String jsonContent = "{\"limits\":{\"memory\":\"1024Mi\",\"cpu\":\"\"},\"requests\":{\"memory\":\"1024Mi\",\"cpu\":\"1\"}}";
    JsonNode defaultValue = Serialization.unmarshal(jsonContent, JsonNode.class);
    objWithDefaultValues.setDefault(defaultValue);
    props.put("o1", objWithDefaultValues);

    JSONSchemaProps objWithoutDefaultValues = new JSONSchemaProps();
    objWithoutDefaultValues.setType("object");
    props.put("o2", objWithoutDefaultValues);

    JObject obj = new JObject(null, "t", props, null, false, defaultConfig, null, Boolean.FALSE, null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals(3, res.getTopLevelClasses().size());
    assertEquals("O1", res.getTopLevelClasses().get(0).getName());
    assertEquals("O2", res.getTopLevelClasses().get(1).getName());
    assertEquals("T", res.getTopLevelClasses().get(2).getName());

    Optional<ClassOrInterfaceDeclaration> clzT = res.getTopLevelClasses().get(2).getClassByName("T");
    assertTrue(clzT.isPresent());
    assertEquals(2, clzT.get().getFields().size());

    Optional<FieldDeclaration> o1Field = clzT.get().getFieldByName("o1");
    assertTrue(o1Field.isPresent());
    FieldDeclaration actualO1Field = o1Field.get();
    VariableDeclarator variableDeclarator = actualO1Field.getVariable(0);
    assertNotNull(variableDeclarator);
    Optional<Expression> initializer = variableDeclarator.getInitializer();
    assertTrue(initializer.isPresent());

    Optional<FieldDeclaration> o2Field = clzT.get().getFieldByName("o2");
    assertTrue(o2Field.isPresent());
    FieldDeclaration actualO2Field = o2Field.get();
    variableDeclarator = actualO2Field.getVariable(0);
    assertNotNull(variableDeclarator);
    initializer = variableDeclarator.getInitializer();
    assertFalse(initializer.isPresent());

    Optional<ClassOrInterfaceDeclaration> clzO1 = res.getTopLevelClasses().get(0).getClassByName("O1");
    assertTrue(clzO1.isPresent());

    Optional<ClassOrInterfaceDeclaration> clzO2 = res.getTopLevelClasses().get(1).getClassByName("O2");
    assertTrue(clzO2.isPresent());
  }

  @Test
  void testExactlyMoreThanOneDuplicatedFieldFailsWithException() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();

    JSONSchemaProps duplicatedFieldObject = new JSONSchemaProps();
    duplicatedFieldObject.setType("boolean");
    duplicatedFieldObject.setDescription("This field is JUST FOR testing purposes.");
    props.put("test_Dup", duplicatedFieldObject);
    duplicatedFieldObject.setDescription("Deprecated: This field is JUST FOR testing purposes.");
    props.put("test Dup", duplicatedFieldObject);
    props.put("test.Dup", duplicatedFieldObject);

    // Assert
    assertThrows(JavaGeneratorException.class, () -> {
      // Act
      JObject obj = new JObject(null, "t", props, null, false, defaultConfig, null, Boolean.FALSE, null);
    },
        "An exception is expected to be thrown when an object contains more that one duplicated field");
  }

  @Test
  void testExactlyDuplicatedFieldNotMarkedAsDeprecatedFailsWithException() {
    // Arrange
    Map<String, JSONSchemaProps> props = new HashMap<>();

    JSONSchemaProps duplicatedFieldObject = new JSONSchemaProps();
    duplicatedFieldObject.setType("boolean");
    duplicatedFieldObject.setDescription("This field is JUST FOR testing purposes.");
    props.put("testDup", duplicatedFieldObject);
    props.put("test-Dup", duplicatedFieldObject);

    // Assert
    assertThrows(JavaGeneratorException.class, () -> {
      // Act
      JObject obj = new JObject(null, "t", props, null, false, defaultConfig, null, Boolean.FALSE, null);
    },
        "An exception is expected to be thrown when an object contains one duplicated field that is not marked as deprecated");
  }

  @Test
  void testExistingJavaTypeObject() {
    // Arrange
    Config config = Config.builder()
        .existingJavaTypes(Collections.singletonMap("v1alpha1.T", "org.test.ExistingJavaType")).build();
    JObject obj = new JObject(
        "v1alpha1",
        "T",
        null,
        null,
        false,
        config,
        null,
        Boolean.FALSE,
        null);

    // Act
    GeneratorResult res = obj.generateJava();

    // Assert
    assertEquals("org.test.ExistingJavaType", obj.getType());
    assertEquals(0, res.getTopLevelClasses().size());
  }
}
