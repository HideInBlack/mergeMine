package org.njupt.util;


import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.util.Objects;
import java.util.Optional;

public class DzyUtils {
    public static void main(String[] args) {

//        // Java 代码字符串
//        String code = ") -> {\n123" +
//                "            //if there is an existing prompt or progressdialog,...\n" +
//                "            if (promptDialogManager.bringCurrentDialogToFront()) {\n" +
//                "                //... just show that\n" +
//                "            } else {\n" +
//                "\n" +
//                "                if ( //confirm timeline during ingest\n"
//              ;
//
//        // 设置 JavaParser
//        JavaParser javaParser = new JavaParser(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver())));
//
//        code = code.replace("\n", "\n _Newline_ ");
//        // 解析代码
//        Optional<CompilationUnit> compilationUnit = javaParser.parse(code).getResult();
//
//        // 检查代码是否正确解析
//        if (compilationUnit.isPresent()) {
//            // 获取 TokenRange，即代码中的所有 tokens
//            TokenRange tokenRange = compilationUnit.get().getTokenRange().get();
//
//            // 遍历 tokens 并打印
//            tokenRange.forEach(token -> {
//                if (!Objects.equals(token.getText().replaceAll("\\s*",""), "")){
//                    System.out.println(token.getText());
//                }
//            });
//        } else {
//            System.out.println("Code not parsed correctly!");
//        }


    }
}
