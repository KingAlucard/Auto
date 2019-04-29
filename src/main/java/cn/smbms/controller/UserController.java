package cn.smbms.controller;

import cn.smbms.pojo.Role;
import cn.smbms.pojo.User;
import cn.smbms.service.role.RoleService;
import cn.smbms.service.role.RoleServiceImpl;
import cn.smbms.service.user.UserService;
import cn.smbms.service.user.UserServiceImpl;
import cn.smbms.tools.Constants;
import cn.smbms.tools.PageSupport;
import com.alibaba.fastjson.JSONArray;
import com.mysql.jdbc.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;

@Controller
@RequestMapping("/user")
public class UserController {
    private static Logger logger = Logger.getLogger(UserController.class);
    @Resource
    private UserService userService;

    @RequestMapping("/login.html")
    public String logion(){
        logger.debug("UserController welcome SMBMS=============");
        return "login";
    }

//    @RequestMapping(value = "/dologin.html",method = RequestMethod.POST)
//    public String doLogin(String userCode, String userPassword,
//                          HttpSession session, HttpServletRequest request){
//            logger.debug("dologin===========================");
//        User user = userService.login(userCode,userPassword);
//            if(user != null){
//                //页面跳转（frame.jsp）
//                session.setAttribute(Constants.USER_SESSION,user);
//                return "redirect:/user/main.html";
//                //response.senRedirect(jsp/frame.jsp)
//            }else {
//                request.setAttribute("error","用户名和密码错误");
//                return "login";
//        }
//    }

    @RequestMapping("/main.html")
    public String main(HttpSession session){
        if(session.getAttribute(Constants.USER_SESSION) != null ){
            return "frame";
        }
        return "login";
    }

    /**
     * 退出登录
     * @param session
     * @return
     */
    @RequestMapping("/LoginOut.html")
    public String LoginOut(HttpSession session){
        session.removeAttribute(Constants.USER_SESSION);
        return "login";
    }


    @RequestMapping("/userlist.html")
    public String userList(@RequestParam(value = "",required = false) String queryname,
                           @RequestParam(value = "",required = false) String queryUserRole,
                           @RequestParam(value = "",required = false) String pageIndex,
                           HttpServletRequest request){
        //查询用户列表
        int UserRole = 0;
        List<User> userList = null;
        //设置页面容量
        int pageSize = Constants.pageSize;
        //当前页码
        int currentPageNo = 1;
        /**
         * http://localhost:8090/SMBMS/userlist.do
         * ----queryUserName --NULL
         * http://localhost:8090/SMBMS/userlist.do?queryname=
         * --queryUserName ---""
         */
        System.out.println("queryUserName servlet--------"+queryname);
        System.out.println("queryUserRole servlet--------"+queryUserRole);
        System.out.println("query pageIndex--------- > " + pageIndex);
        if(queryname == null){
            queryname = "";
        }
        if(queryUserRole != null && !queryUserRole.equals("")){
            UserRole = Integer.parseInt(queryUserRole);
        }

        if(pageIndex != null){
            try{
                currentPageNo = Integer.valueOf(pageIndex);
            }catch(NumberFormatException e){
//                response.sendRedirect("error.jsp");
                return "error";
            }
        }
        //总数量（表）
        int totalCount	= userService.getUserCount(queryname,UserRole);
        //总页数
        PageSupport pages=new PageSupport();
        pages.setCurrentPageNo(currentPageNo);
        pages.setPageSize(pageSize);
        pages.setTotalCount(totalCount);

        int totalPageCount = pages.getTotalPageCount();

        //控制首页和尾页
        if(currentPageNo < 1){
            currentPageNo = 1;
        }else if(currentPageNo > totalPageCount){
            currentPageNo = totalPageCount;
        }


        userList = userService.getUserList(queryname,UserRole,currentPageNo, pageSize);
        request.setAttribute("userList", userList);
        List<Role> roleList = null;
        RoleService roleService = new RoleServiceImpl();
        roleList = roleService.getRoleList();
        request.setAttribute("roleList", roleList);
        request.setAttribute("queryUserName", queryname);
        request.setAttribute("queryUserRole", UserRole);
        request.setAttribute("totalPageCount", totalPageCount);
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("currentPageNo", currentPageNo);
        return "userlist";
    }


    @RequestMapping(value = "/userAdd.html")
    public String userAdd(@ModelAttribute("user") User user){
        return "useradd";
    }

    /**
     * 注册文件上传
     * @param user
     * @param session
     * @param attr
     * @return
     */
    @RequestMapping(value = "/addSave.html",method = RequestMethod.POST)
    public String userSave(User user,HttpSession session,@RequestParam(value = "attr" ,required = false) MultipartFile attr){
        user.setCreatedBy(((User)session.getAttribute(Constants.USER_SESSION)).getId());
        user.setCreationDate(new Date());

        String idPicPath=null;
        //判断是否为空
        if(!attr.isEmpty()){
            //获取文件的路径  File.separator系统的自适应分隔符
            String filePath=session.getServletContext().getRealPath("statics"+ File.separator +"uploadfiles");
            //获取源文件名
            String fileOldName=attr.getOriginalFilename();
            //获取文件的后缀
//            String sufix=fileOldName.substring(fileOldName.lastIndexOf(".")+1,fileOldName.length());
            String sufix= FilenameUtils.getExtension(fileOldName);
            List<String> sufixs= Arrays.asList(new String[]{"jpg","png","jpeg","pneg"});
            if(attr.getSize()>500000){
                session.setAttribute("uploadFileError","文件太大了");
                return "useradd";
            }else if(sufixs.contains(sufix)){
                //重新命名，目的就是解决重名和字符乱码问题
                String fileName=System.currentTimeMillis()+new Random().nextInt(1000000)+"_person."+sufix;
                File file=new File(filePath,fileName);
                if(!file.exists()){
                    file.mkdirs();
                }
                try {
                    attr.transferTo(file);

                }catch (Exception e){
                    e.printStackTrace();
                    session.setAttribute("uploadFileError","上传失败");
                    return "useradd";
                }
                idPicPath=filePath+File.separator+fileName;
                System.out.println("=====>"+idPicPath);
            }else{
                session.setAttribute("uploadFileError","文件格式不对");
                return "useradd";
            }
        }
        user.setCreationDate(new Date());
        user.setCreatedBy(((User)session.getAttribute(Constants.USER_SESSION)).getId());
        user.setIdPicPath(idPicPath);
        if(userService.add(user)){
            return "redirect:/user/userlist.html";
        }
        return "useradd";
    }

    //删除
    @RequestMapping(value = "/userDel.html")
    public String userDel(@PathVariable String id) {
        System.out.println("============================编号"+id);
        if (userService.deleteUserById(Integer.parseInt(id))) {
            return "redirect:/user/userlist.html";
        }
        return "userlist";

    }


    @RequestMapping(value = "/userSele.html/{uid}")
    public String userSele(@PathVariable String uid, Model model){
        User user = userService.getUserById(uid);
        model.addAttribute("user",user);
        return "userview";
    }

    @RequestMapping(value = "/userUpdate.html/{uid}")
    public String userUpdate(@PathVariable String uid, Model model){
        User user = userService.getUserById(uid);
        model.addAttribute("user",user);
        return "usermodify";
    }

    @RequestMapping(value = "/userUpSave.html",method = RequestMethod.POST)
    public String userUpSave(User user,HttpSession session){

        user.setModifyDate(new Date());
        user.setModifyBy(((User)session.getAttribute(Constants.USER_SESSION)).getId());


        if (userService.modify(user)){
            return "redirect:/user/userlist.html";
        }
        return "usermodify";
    }


    /**
     * ajax异步验证
     */
    @RequestMapping("/userDo.html")
    @ResponseBody
    public String userDo(String userCode){
    //判断用户账号是否可用
        HashMap<String, String> resultMap = new HashMap<String, String>();
        if(StringUtils.isNullOrEmpty(userCode)){
            //userCode == null || userCode.equals("")
            resultMap.put("userCode", "exist");
        }else{
            User user = userService.selectUserCodeExist(userCode);
            if(null != user){
                resultMap.put("userCode","exist");
            }else{
                resultMap.put("userCode", "notexist");
            }
        }

        //把resultMap转为json字符串 输出
       return JSONArray.toJSONString(resultMap);
    }
}



