package org.njupt.core;

import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassAnalyzer {
    public static Map<String, Map<String, String>> analyzeClass(String javaSource) {
        // 创建 AST 解析器
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(javaSource.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        // 解析代码，生成 AST
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        // 存储类的相关信息
        Map<String, Map<String, String>> result = new HashMap<>();

        // 获取所有类型声明（类、接口、枚举等）
        List<TypeDeclaration> types = cu.types();
        for (TypeDeclaration type : types) {
            String fullyQualifiedName = type.getName().getFullyQualifiedName();

            // 存储当前类的成员信息
            Map<String, String> classInfo = new HashMap<>();
            StringBuilder fields = new StringBuilder();
            StringBuilder methods = new StringBuilder();

            // 获取成员变量（字段）
            FieldDeclaration[] fieldDeclarations = type.getFields();
            for (FieldDeclaration field : fieldDeclarations) {
                String fieldType = field.getType().toString();
                for (Object fragment : field.fragments()) {
                    VariableDeclarationFragment varFragment = (VariableDeclarationFragment) fragment;
                    fields.append(fieldType).append(" ").append(varFragment.getName()).append("; ");
                }
            }
            classInfo.put("field", fields.toString());

            // 获取方法
            MethodDeclaration[] methodDeclarations = type.getMethods();
            for (MethodDeclaration method : methodDeclarations) {
                String returnType = method.getReturnType2().toString();
                String methodName = method.getName().toString();
                String methodSignature = returnType + " " + methodName + "(";

                // 获取方法参数类型
                List<SingleVariableDeclaration> parameters = method.parameters();
                for (int i = 0; i < parameters.size(); i++) {
                    SingleVariableDeclaration parameter = parameters.get(i);
                    if (i > 0) methodSignature += ", ";
                    methodSignature += parameter.getType().toString() + " " + parameter.getName();
                }
                methodSignature += ")";

                methods.append(methodSignature).append(" ");
            }
            classInfo.put("method", methods.toString());

            // 将信息添加到结果Map
            result.put(fullyQualifiedName, classInfo);
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        // 1.输入 Java 类的源代码
        // String javaSource = "public class Example { private int a; private String b; public void methodOne() {} public String methodTwo(int x) { return \"\"; }}";
        String javaSource = new String(Files.readAllBytes(Paths.get( "G:\\now\\2024merge\\mergeMine\\src\\main\\java\\org\\njupt\\core\\DatasetCollector.java")));

        // 2.获取类的成员和方法信息
        Map<String, Map<String, String>> classInfo = analyzeClass(javaSource);

        // 3.输出结果
        for (String className : classInfo.keySet()) {
            System.out.println("Class: " + className);
            Map<String, String> info = classInfo.get(className);
            System.out.println("Fields: " + info.get("field"));
            System.out.println("Methods: " + info.get("method"));
        }
    }
}
