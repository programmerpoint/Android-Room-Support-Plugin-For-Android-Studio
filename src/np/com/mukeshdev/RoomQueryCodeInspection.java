package np.com.mukeshdev;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RoomQueryCodeInspection extends BaseJavaLocalInspectionTool {

    private static final String ANNOTATION_QUERY = "@Query";
    private RoomQueryLocalQuickFix roomQueryLocalQuickFix=new RoomQueryLocalQuickFix();

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
                PsiModifierList psiModifierList = method.getModifierList();

                String annotation = psiModifierList.toString();

                if (annotation.contains(ANNOTATION_QUERY)) {

                    //Remove UnWanted String
                    //so we get only the sql query String
                    annotation = annotation.substring(annotation.indexOf(ANNOTATION_QUERY) + ANNOTATION_QUERY.length());

                    PsiParameterList methodParameterList = method.getParameterList();
                    List<String> methodParameterNameList = new ArrayList<>();

                    for (PsiParameter methodParameter : methodParameterList.getParameters()) {
                        methodParameterNameList.add(methodParameter.getName());
                    }

                    String queryAnnotationText;
                    int index;
                    String annotationVariable;

                    queryAnnotationText = annotation;
                    index = queryAnnotationText.indexOf(":");

                    while (index >= 0 && queryAnnotationText.length() > 0) {

                        int annotationParameterEndIndex = queryAnnotationText.indexOf(" ", index);

                        if (annotationParameterEndIndex == -1) {
                            annotationParameterEndIndex = queryAnnotationText.indexOf("\"", index);
                        }

                        if (annotationParameterEndIndex == -1) {
                            break;
                        }

                        annotationVariable = queryAnnotationText.substring(index + ":".length(), annotationParameterEndIndex);

                        if (!methodParameterNameList.contains(annotationVariable)) {

                            holder.registerProblem(method, "Cannot find method parameters for :" + annotationVariable +
                                            " Error: Each bind variable in the query must have a matching method parameter."
                                    , roomQueryLocalQuickFix);
                        }


                        queryAnnotationText = queryAnnotationText.substring(index + ":".length());
                        index = queryAnnotationText.indexOf(":");
                    }


                }
            }

        };


    }

}
