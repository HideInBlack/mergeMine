package org.njupt.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

/**
 * 在使用 Eclipse JDT 来分析一个 Java 类的直接依赖关系时，我们可以通过解析 AST（抽象语法树）来提取信息。
 * 2025年1月7日 01点20分
 */
public class DependencyAnalyzer {

    public static void main(String[] args) {
        // 替换为你本地的 Java 文件路径
        String filePath = "F:\\spaceLearn\\research\\mergeMine\\src\\main\\java\\org\\njupt\\util\\SampleClass.java";

        try {
            String javaSource = new String(Files.readAllBytes(Paths.get(filePath)));
            analyzeDependencies(javaSource);
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }

    public static void analyzeDependencies(String javaSource) {
        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setSource(javaSource.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);

        CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

        if (compilationUnit.getPackage() != null) {
            System.out.println("Package: " + compilationUnit.getPackage().getName());
        }

        Set<String> dependencies = new HashSet<>();

        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(ImportDeclaration node) {
                // 添加完整限定名
                dependencies.add(node.getName().getFullyQualifiedName());
                return super.visit(node);
            }

            @Override
            public boolean visit(SimpleType node) {
                // 解析绑定信息，获取完整限定名
                ITypeBinding binding = node.resolveBinding();
                if (binding != null) {
                    dependencies.add(binding.getQualifiedName());
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(QualifiedType node) {
                ITypeBinding binding = node.resolveBinding();
                if (binding != null) {
                    dependencies.add(binding.getQualifiedName());
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(FieldDeclaration node) {
                ITypeBinding binding = node.getType().resolveBinding();
                if (binding != null) {
                    dependencies.add(binding.getQualifiedName());
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                // 方法返回类型
                ITypeBinding returnType = node.getReturnType2() != null ? node.getReturnType2().resolveBinding() : null;
                if (returnType != null) {
                    dependencies.add(returnType.getQualifiedName());
                }
                return super.visit(node);
            }
        });

        System.out.println("\nDirectly depends on:");
        dependencies.forEach(System.out::println);
    }
}
