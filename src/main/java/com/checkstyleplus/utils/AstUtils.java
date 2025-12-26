package com.checkstyleplus.utils;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Utility class for AST node traversal and identifier search.
 */
public class AstUtils {

    public static DetailAST findIdentAtLineWithText(DetailAST root, int line1Based, String name) {
        if (root == null || name == null) return null;
        return dfs(root, line1Based, name);
    }

    private static DetailAST dfs(DetailAST node, int line, String name) {
        if (node == null) return null;

        if (node.getType() == TokenTypes.IDENT
                && node.getLineNo() == line
                && name.equals(node.getText())) {
            return node;
        }

        if ((node.getType() == TokenTypes.METHOD_DEF ||
             node.getType() == TokenTypes.CLASS_DEF ||
             node.getType() == TokenTypes.VARIABLE_DEF ||
             node.getType() == TokenTypes.PARAMETER_DEF)
                && node.getLineNo() == line) {
            DetailAST id = node.findFirstToken(TokenTypes.IDENT);
            if (id != null && name.equals(id.getText())) return id;
        }

        for (DetailAST c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
            DetailAST hit = dfs(c, line, name);
            if (hit != null) return hit;
        }
        return null;
    }
}
