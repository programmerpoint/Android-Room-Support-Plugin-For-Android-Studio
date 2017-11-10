package np.com.mukeshdev;

import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Check if Classes annotated with @Database extend RoomDatabase
 * (android.arch.persistence.room.RoomDatabase)
 */
public class RoomDatabaseCodeInspection extends BaseJavaLocalInspectionTool {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Room Database Helper";
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

    private boolean checkForRoomDatabaseClassExtended(PsiClass[] PsiClasses) {
        for (PsiClass psiClass : PsiClasses) {
            System.out.println(psiClass.getName());
            if (psiClass.getName() != null && psiClass.getName().equals("RoomDatabase")) {
                //System.out.println(psiClassType.getClassName());
                return true;
            }

            if (checkForRoomDatabaseClassExtended(psiClass.getSupers())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitClass(PsiClass aClass) {
                super.visitClass(aClass);

                try {

                    PsiModifierList psiClassModifierList = aClass.getModifierList();

                    if (psiClassModifierList == null) {
                        return;
                    }

                    PsiAnnotation psiClassAnnotation =
                            psiClassModifierList.findAnnotation("android.arch.persistence.room.Database");

                    if (psiClassAnnotation == null) {
                        return;
                    }

                    PsiClass[] psiClassSuper = aClass.getSupers();

                    if (!checkForRoomDatabaseClassExtended(psiClassSuper)) {
                        holder.registerProblem(aClass.getModifierList(),
                                "Error:Classes annotated with @Database should extend " +
                                        "android.arch.persistence.room.RoomDatabase"
                                , ProblemHighlightType.GENERIC_ERROR);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };


    }
}