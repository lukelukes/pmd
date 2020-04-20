/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.ast;

import apex.jorje.semantic.ast.expression.SuperMethodCallExpression;

public class ASTSuperMethodCallExpression extends AbstractApexNode<SuperMethodCallExpression> {

    ASTSuperMethodCallExpression(SuperMethodCallExpression superMethodCallExpression) {
        super(superMethodCallExpression);
    }

    @Override
    public Object jjtAccept(ApexParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
