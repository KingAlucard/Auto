package cn.smbms.dao.role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import cn.smbms.dao.BaseDao;
import cn.smbms.pojo.Role;

public class RoleDaoImpl implements RoleDao{

	@Override
	public List<Role> getRoleList(Connection connection) throws Exception {
		// TODO Auto-generated method stub
		PreparedStatement pstm = null;
		ResultSet rs = null;
		List<Role> roleList = new ArrayList<Role>();
		if(connection != null){
			String sql = "select * from smbms_role";
			Object[] params = {};
			rs = BaseDao.execute(connection, pstm, rs, sql, params);
			while(rs.next()){
				Role _role = new Role();
				_role.setId(rs.getInt("id"));
				_role.setRoleCode(rs.getString("roleCode"));
				_role.setRoleName(rs.getString("roleName"));
				roleList.add(_role);
			}
			BaseDao.closeResource(null, pstm, rs);
		}
		
		return roleList;
	}

}
