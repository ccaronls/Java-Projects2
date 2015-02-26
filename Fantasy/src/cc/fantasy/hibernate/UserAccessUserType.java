package cc.fantasy.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import cc.fantasy.model.type.UserAccess;

public class UserAccessUserType implements UserType {

    public static final int[] SQL_TYPES = {Types.VARCHAR};
    
    public Object assemble(Serializable cached, Object arg1) throws HibernateException {
        return cached;
    }

    public Object deepCopy(Object arg0) throws HibernateException {
        return arg0;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    public boolean equals(Object arg0, Object arg1) throws HibernateException {
        return arg0 == arg1;
    }

    public int hashCode(Object arg0) throws HibernateException {
        if (arg0 == null)
            return 0;
        return arg0.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object arg2) throws HibernateException, SQLException {
        String name = rs.getString(names[0]);
        if (rs.wasNull())
            return UserAccess.NONE;
        return UserAccess.valueOf(name);
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
        if (value == null)
            st.setNull(index, Types.VARCHAR);
        else
            st.setString(index, ((UserAccess)value).name());
    }

    public Object replace(Object orig, Object target, Object owner) throws HibernateException {
        return orig;
    }

    public Class returnedClass() {
        return UserAccess.class;
    }

    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    
    
}
