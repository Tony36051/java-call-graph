package com.huawei.it.unistar.jp;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 试验代码
 */
public class Demo {


    public static void main(String[] args) throws FileNotFoundException {
        String m = "com.huawei.it.unistar.cpi.upl.maintenance.other.vo.CpartDiscountInfoVO.getDiscount7()";
        Pattern pattern = Pattern.compile(".*vo");
        System.out.println(pattern.matcher(m).matches());
    }

}
