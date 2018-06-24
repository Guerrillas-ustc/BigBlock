import cn.edu.ustc.Guerrillas.BigBlock.server.CassandraDao;
import org.junit.Test;

import java.lang.reflect.Constructor;

public class ServerTest {
    static <T> T newInstance(Class<T> clazz) {
        Constructor<T> publicConstructor;
        try {
            publicConstructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            try {
                // try private constructor
                Constructor<T> privateConstructor = clazz.getDeclaredConstructor();
                privateConstructor.setAccessible(true);
                return privateConstructor.newInstance();
            } catch (Exception e1) {
                throw new IllegalArgumentException("Can't create an instance of " + clazz, e);
            }
        }
        try {
            return publicConstructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't create an instance of " + clazz, e);
        }
    }
    @Test
    public void testBlock() {
        try {
            CassandraDao.BlockObject object = newInstance(CassandraDao.BlockObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
