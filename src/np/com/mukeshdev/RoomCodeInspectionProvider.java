package np.com.mukeshdev;

import com.intellij.codeInspection.InspectionToolProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Room Code Inspection Provider
 */
public class RoomCodeInspectionProvider implements InspectionToolProvider {

    @NotNull
    @Override
    public Class[] getInspectionClasses() {
        return new Class[]{
                RoomQueryCodeInspection.class,
                EntityCodeInspection.class,
                RoomDatabaseCodeInspection.class
        };
    }
}
