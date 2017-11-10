package np.com.mukeshdev;

import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Check for error in Query
 * Check if bind variable in query has matching parameter
 * Check if Table Name is Correct in Query
 */
public class RoomQueryCodeInspection extends BaseJavaLocalInspectionTool {

    private static final String ANNOTATION_QUERY = "@Query";
    /*private static final String ANNOTATION_DELETE = "@Delete";*/
    private static final String FROM = " from ";
    private static final String SELECT = "select";

    private static final String ANNOTATION_QUALIFIED_NAME_QUERY = "android.arch.persistence.room.Query";
    private static final String ANNOTATION_QUALIFIED_NAME_DELETE = "android.arch.persistence.room.Delete";
    private static final String ANNOTATION_QUALIFIED_NAME_INSERT = "android.arch.persistence.room.Insert";

    private List<String> ListOfRoomAnnotationToCheck =
            Arrays.asList(ANNOTATION_QUALIFIED_NAME_QUERY,
                    ANNOTATION_QUALIFIED_NAME_DELETE,
                    ANNOTATION_QUALIFIED_NAME_INSERT);

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

                    String annotation = CheckForRoomAnnotation(psiModifierList);

                    if (annotation != null) {
                        switch (annotation) {
                            case ANNOTATION_QUALIFIED_NAME_QUERY:
                                RunQueryCodeInspection(method,
                                        psiModifierList.findAnnotation(ANNOTATION_QUALIFIED_NAME_QUERY).getText(), holder);
                                break;
                            case ANNOTATION_QUALIFIED_NAME_DELETE:
                                RunDeleteQueryCodeInspection(method, holder);
                                break;
                            case ANNOTATION_QUALIFIED_NAME_INSERT:
                                RunInsertQueryCodeInspection(method, holder);
                                break;
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };


    }

    private String CheckForRoomAnnotation(PsiModifierList psiModifierList) {
        PsiAnnotation psiAnnotation;

        for (String RoomAnnotationToCheck : ListOfRoomAnnotationToCheck) {
            psiAnnotation = psiModifierList.findAnnotation(RoomAnnotationToCheck);
            if (psiAnnotation != null) {
                return RoomAnnotationToCheck;
            }
        }

        return null;

    }

    //TODO add more inspection
    private void RunInsertQueryCodeInspection(PsiMethod method, ProblemsHolder holder) {
        if (method.getParameterList().getParameters().length == 0) {
            holder.registerProblem(method.getModifierList(),
                    "Error: Method annotated with @Insert but does not have any parameters to insert."
                    , ProblemHighlightType.GENERIC_ERROR);
        }
    }

    //TODO add more inspection
    private void RunDeleteQueryCodeInspection(PsiMethod method, ProblemsHolder holder) {
        if (method.getParameterList().getParameters().length == 0) {
            holder.registerProblem(method.getModifierList(),
                    "Error: Method annotated with @Delete but does not have any parameters to delete."
                    , ProblemHighlightType.GENERIC_ERROR);
        }
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

    /*
    * Do not work for all case
    * Only works if return type has Table Class
    *
    * It check for return type and check for @Entity
    * to determine the valid table name and check for table name
    * in @Query
    *
    * */
    //TODO Update to make it work for other case
    private void runQueryTableNameCodeInspection(PsiMethod method, String annotation, @NotNull ProblemsHolder holder) {
        String queryAnnotationText = annotation;
        queryAnnotationText = queryAnnotationText.toLowerCase();
        if (queryAnnotationText.contains(FROM) && queryAnnotationText.contains(SELECT) && !queryAnnotationText.contains(".")) {
            int indexOfFrom = queryAnnotationText.indexOf(FROM);

            int indexOfTableName = indexOfFrom + FROM.length();

            queryAnnotationText = queryAnnotationText.substring(indexOfTableName);

            int endIndexOfTableName = queryAnnotationText.indexOf(" ");

            if (endIndexOfTableName == -1) {
                endIndexOfTableName = queryAnnotationText.indexOf("\"");
            }

            if (endIndexOfTableName > 0) {
                String tableName = queryAnnotationText.substring(0, endIndexOfTableName);


                /*
                * Check in Entity Class for table name
                * and see if it match with query table Name
                * */
                PsiClass entityClassOfTheQuery;

                PsiType psiTypes = method.getReturnType();

                if (psiTypes != null) {
                    //Check if psiTypes is List<>
                    if (psiTypes.getCanonicalText().contains("<")) {
                        psiTypes = PsiUtil.extractIterableTypeParameter(psiTypes, false);
                    }
                } else {
                    return;
                }

                entityClassOfTheQuery = PsiTypesUtil.getPsiClass(psiTypes);

                if (entityClassOfTheQuery != null) {
                    //System.out.println("->" + entityClassOfTheQuery.getQualifiedName());

                    PsiModifierList psiModifierList =
                            entityClassOfTheQuery.getModifierList();

                    if (psiModifierList != null) {
                        PsiAnnotation psiAnnotationOfEntityClass
                                = psiModifierList.findAnnotation("android.arch.persistence.room.Entity");

                        if (psiAnnotationOfEntityClass != null) {
                            PsiNameValuePair[] psiEntityClassAnnotationNameValuePairs = psiAnnotationOfEntityClass.getParameterList().getAttributes();

                            String psiNameValuePairOfAnnotationParameterName;

                            for (PsiNameValuePair psiNameValuePair : psiEntityClassAnnotationNameValuePairs) {

                                psiNameValuePairOfAnnotationParameterName = psiNameValuePair.getName();

                                if (psiNameValuePairOfAnnotationParameterName != null) {
                                    if (psiNameValuePairOfAnnotationParameterName.equals("tableName")) {
                                        String TableNameFromEntityClass = psiNameValuePair.getLiteralValue();

                                        if (TableNameFromEntityClass != null)
                                            if (!TableNameFromEntityClass.equalsIgnoreCase(tableName)) {
                                                holder.registerProblem(method.getModifierList(), "No Such table: " + tableName +
                                                                ".Table Name of Entity Class(" + entityClassOfTheQuery.getQualifiedName() + ")is " + TableNameFromEntityClass

                                                        , ProblemHighlightType.GENERIC_ERROR);
                                            }
                                        return;
                                    }
                                }
                            }

                            String className = entityClassOfTheQuery.getName();

                            if (className != null)
                                if (!className.equalsIgnoreCase(tableName)) {
                                    holder.registerProblem(method.getModifierList(), "No Such table: " + tableName +
                                                    ".Table Name of Entity Class(" + entityClassOfTheQuery.getQualifiedName() + ")is " + entityClassOfTheQuery.getName()

                                            , ProblemHighlightType.GENERIC_ERROR);
                                }


                        }

                    }

                } else {
                    System.out.println("Not class ->" + method.getReturnType());
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
                annotationParameterEndIndex = queryAnnotationText.indexOf("\n", indexOfQueryVariable);
            }


            if (annotationParameterEndIndex == -1) {
                annotationParameterEndIndex = queryAnnotationText.indexOf("\"", indexOfQueryVariable);
            }


            if (annotationParameterEndIndex == -1) {
                break;
            }


            annotationVariable =
                    queryAnnotationText.substring(indexOfQueryVariable + ":".length(), annotationParameterEndIndex);

            annotationVariable = annotationVariable.replace("\n", "");
            annotationVariable = annotationVariable.replace(" ", "");
            annotationVariable = annotationVariable.replace("\"", "");

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
