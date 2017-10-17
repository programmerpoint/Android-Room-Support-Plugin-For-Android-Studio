package np.com.mukeshdev;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Check for error in Query
 * Check if bind variable in query has matching parameter
 */
public class RoomQueryCodeInspection extends BaseJavaLocalInspectionTool {

    private static final String ANNOTATION_QUERY = "@Query";
    private static final String ANNOTATION_DELETE = "@Delete";
    public static final String FROM = " from ";
    public static final String SELECT = "select";
    /*private RoomQueryLocalQuickFix roomQueryLocalQuickFix = new RoomQueryLocalQuickFix();*/

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Room Helper";
    }

    @Nls
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Android Room";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitMethod(PsiMethod method) {
                super.visitMethod(method);

                try {
                    PsiModifierList psiModifierList = method.getModifierList();

                    String annotation = psiModifierList.toString();

                    if (annotation.contains(ANNOTATION_QUERY)) {
                        RunQueryCodeInspection(method, annotation, holder);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };


    }

    private void RunQueryCodeInspection(PsiMethod method, String annotation, @NotNull ProblemsHolder holder) {
        //Remove UnWanted String
        //so we get only the sql query String
        annotation = annotation.substring(annotation.indexOf(ANNOTATION_QUERY) + ANNOTATION_QUERY.length());

        PsiParameterList methodParameterList = method.getParameterList();
        List<String> methodParameterNameList = new ArrayList<>();

        for (PsiParameter methodParameter : methodParameterList.getParameters()) {
            methodParameterNameList.add(methodParameter.getName());
        }

        runQueryBindVariableCodeInspection(method, annotation, holder, methodParameterNameList);

        runQueryTableNameCodeInspection(method, annotation, holder);

    }

    private void runQueryTableNameCodeInspection(PsiMethod method, String annotation, @NotNull ProblemsHolder holder) {
        String queryAnnotationText = annotation;
        queryAnnotationText = queryAnnotationText.toLowerCase();
        if (queryAnnotationText.contains(FROM) && queryAnnotationText.contains(SELECT)) {
            int indexOfFrom = queryAnnotationText.indexOf(FROM);

            int indexOfTableName = indexOfFrom + FROM.length();

            queryAnnotationText = queryAnnotationText.substring(indexOfTableName);

            int endIndexOfTableName = queryAnnotationText.indexOf(" ");

            if (endIndexOfTableName == -1) {
                endIndexOfTableName = queryAnnotationText.indexOf("\"");
            }

            if (endIndexOfTableName > 0) {
                String tableName = queryAnnotationText.substring(0, endIndexOfTableName);

                String methodReturnType = method.getReturnType().toString().toLowerCase();

                StringTokenizer st = new StringTokenizer(methodReturnType, ":");
                st.nextToken();
                methodReturnType = st.nextToken();

                methodReturnType=methodReturnType.replace("list<", "");
                methodReturnType=methodReturnType.replace(">", "");

                if (!methodReturnType.equalsIgnoreCase(tableName)) {
                    holder.registerProblem(method.getModifierList(), "No Such table: " + tableName
                            , ProblemHighlightType.GENERIC_ERROR);
                }
            }

        }
    }

    private void runQueryBindVariableCodeInspection(PsiMethod method, String annotation, @NotNull ProblemsHolder holder, List<String> methodParameterNameList) {
        String queryAnnotationText = annotation;
        int indexOfQueryVariable = queryAnnotationText.indexOf(":");
        String annotationVariable;
        int annotationParameterEndIndex;

        while (indexOfQueryVariable >= 0 && queryAnnotationText.length() > 0) {

            annotationParameterEndIndex = queryAnnotationText.indexOf(" ", indexOfQueryVariable);

            if (annotationParameterEndIndex == -1) {
                annotationParameterEndIndex = queryAnnotationText.indexOf("\"", indexOfQueryVariable);
            }

            if (annotationParameterEndIndex == -1) {
                break;
            }

            annotationVariable =
                    queryAnnotationText.substring(indexOfQueryVariable + ":".length(), annotationParameterEndIndex);

            if (!methodParameterNameList.contains(annotationVariable)) {

                holder.registerProblem(method.getModifierList(), "Cannot find method parameters " +
                                "for variable in query:" + annotationVariable
                        , ProblemHighlightType.GENERIC_ERROR);
            }

            queryAnnotationText = queryAnnotationText.substring(indexOfQueryVariable + ":".length());
            indexOfQueryVariable = queryAnnotationText.indexOf(":");
        }
    }

}
