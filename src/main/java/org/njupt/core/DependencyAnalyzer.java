package org.njupt.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

/**
 * 在使用 Eclipse JDT 来分析一个 Java 类的直接依赖关系时，我们可以通过解析 AST（抽象语法树）来提取信息。
 * 2025年1月7日 01点20分
 */
public class DependencyAnalyzer {

    private static final Set<String> JAVA_PACKAGES = Set.of(
            "java", "javax", "org.slf4j", "org.apache"
    );

    public static void main(String[] args) {

        try {
            // 替换为你本地的 Java 文件路径
            System.out.println(analyzeDependencies(new String(Files.readAllBytes(Paths.get( "G:\\now\\2024merge\\mergeMine\\src\\main\\java\\org\\njupt\\core\\DatasetCollector.java")))));
            System.out.println(analyzeDependencies(new String(Files.readAllBytes(Paths.get( "G:\\now\\2024merge\\mergeMine\\src\\main\\java\\org\\njupt\\core\\Dataset50Collector.java")))));
            System.out.println(analyzeDependencies(new String(Files.readAllBytes(Paths.get( "G:\\now\\2024merge\\mergeMine\\src\\main\\java\\org\\njupt\\core\\KeyContextCollector.java")))));
            System.out.println(analyzeDependencies(new String(Files.readAllBytes(Paths.get( "G:\\now\\2024merge\\mergeMine\\src\\main\\java\\org\\njupt\\core\\KeyInformationCollector.java")))));
            System.out.println(analyzeDependencies(new String(Files.readAllBytes(Paths.get( "G:\\now\\2024merge\\mergeMine\\src\\main\\java\\org\\njupt\\util\\DzyUtils.java")))));


        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }


    public static Map<String, Set<String>> analyzeDependencies(String javaSource) {

        // 用来存储类与其依赖的映射
        Map<String, Set<String>> result = new HashMap<>();

        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setSource(javaSource.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);

        CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

        if (compilationUnit.getPackage() != null) {
            // System.out.println("Package: " + compilationUnit.getPackage().getName());
        }

        // 存储每个类的依赖集合
        Set<String> dependencies = new HashSet<>();
        String className = null;

        // 获取包名
        String packageName = compilationUnit.getPackage() != null ? compilationUnit.getPackage().getName().toString() : "";

        // 获取类名 (假设该 Java 文件中只有一个类)
        for (Object type : compilationUnit.types()) {
            if (type instanceof TypeDeclaration) {
                TypeDeclaration typeDeclaration = (TypeDeclaration) type;
                className = typeDeclaration.getName().getFullyQualifiedName();
                break; // 假设只有一个类
            }
        }

        if (className != null) {
            String fullyQualifiedClassName = packageName.isEmpty() ? className : packageName + "." + className;

            compilationUnit.accept(new ASTVisitor() {
                // 遍历导入语句
                @Override
                public boolean visit(ImportDeclaration node) {
                    // 获取导入类的全限定名
                    dependencies.add(node.getName().getFullyQualifiedName());
                    return super.visit(node);
                }

                // 遍历类名类型
                @Override
                public boolean visit(SimpleType node) {
                    // 解析绑定信息，获取完整限定名
                    ITypeBinding binding = node.resolveBinding();
                    if (binding != null) {
                        dependencies.add(binding.getQualifiedName());
                    }
                    return super.visit(node);
                }

                // 遍历限定类型
                @Override
                public boolean visit(QualifiedType node) {
                    ITypeBinding binding = node.resolveBinding();
                    if (binding != null) {
                        dependencies.add(binding.getQualifiedName());
                    }
                    return super.visit(node);
                }

                // 遍历字段声明
                @Override
                public boolean visit(FieldDeclaration node) {
                    ITypeBinding binding = node.getType().resolveBinding();
                    if (binding != null) {
                        dependencies.add(binding.getQualifiedName());
                    }
                    return super.visit(node);
                }

                // 遍历方法声明
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

            // 过滤掉 Java 标准库的依赖
            Set<String> filteredDependencies = new HashSet<>();
            for (String dependency : dependencies) {
                if (!isJavaPackage(dependency)) {
                    filteredDependencies.add(dependency);
                }
            }

            // 将当前类的全限定类名与其依赖项添加到结果中
            result.put(fullyQualifiedClassName, filteredDependencies);
        }

        // 返回结果
        return result;
    }

    // 判断依赖是否属于 Java 原生包
    private static boolean isJavaPackage(String qualifiedName) {
        for (String javaPackage : JAVA_PACKAGES) {
            if (qualifiedName.startsWith(javaPackage + ".")) {
                return true;
            }
        }
        return false;
    }
}
