/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.ast;

import apex.jorje.semantic.ast.expression.TernaryExpression;

public class ASTTernaryExpression extends AbstractApexNode<TernaryExpression> {

    ASTTernaryExpression(TernaryExpression ternaryExpression) {
        super(ternaryExpression);
    }

    @Override
    public Object jjtAccept(ApexParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
