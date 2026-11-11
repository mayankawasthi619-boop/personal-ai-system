package com.personalai.common;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class PgVectorType implements UserType<PGvector> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<PGvector> returnedClass() {
        return PGvector.class;
    }

    @Override
    public boolean equals(PGvector x, PGvector y) {
        if (x == y) return true;
        if (x == null || y == null) return false;
        return x.equals(y);
    }

    @Override
    public int hashCode(PGvector x) {
        return x != null ? x.hashCode() : 0;
    }

    @Override
    public PGvector nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        Object val = rs.getObject(position);
        if (val == null) return null;
        if (val instanceof PGvector v) return v;
        if (val instanceof org.postgresql.util.PGobject o) return new PGvector(o.getValue());
        return new PGvector(val.toString());
    }

    @Override
    public void nullSafeSet(PreparedStatement st, PGvector value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER, "vector");
        } else {
            st.setObject(index, value, Types.OTHER);
        }
    }

    @Override
    public PGvector deepCopy(PGvector value) {
        if (value == null) return null;
        return new PGvector(value.toArray());
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(PGvector value) {
        return value != null ? value.toArray() : null;
    }

    @Override
    public PGvector assemble(Serializable cached, Object owner) {
        return cached != null ? new PGvector((float[]) cached) : null;
    }
}
