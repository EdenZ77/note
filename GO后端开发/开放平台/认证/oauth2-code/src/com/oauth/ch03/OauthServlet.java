package com.oauth.ch03;

import com.my.util.URLParamsUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * **
 * 使用此类来模拟【授权服务】
 */

@WebServlet("/OauthServlet-ch03")
public class OauthServlet extends HttpServlet {

    //模拟授权码、令牌等数据存储
    static Map<String,String> codeMap =  new HashMap<String, String>();
    static Map<String,String[]> codeScopeMap =  new HashMap<String, String[]>();

    static Map<String,String> tokenMap =  new HashMap<String, String>();
    static Map<String,String[]> tokenScopeMap =  new HashMap<String, String[]>();

    static Map<String,String> refreshTokenMap =  new HashMap<String, String>();

    static Map<String,String> appMap =  new HashMap<String, String>();

    static Map<String,String> reqidMap =  new HashMap<String, String>();


    static {

        //模拟第三方软件注册之后的数据库存储
        appMap.put("app_id","APPID_RABBIT");
        appMap.put("app_secret","APPSECRET_RABBIT");
        appMap.put("redirect_uri","http://localhost:8080/AppServlet-ch03");
        appMap.put("scope","today history");

    }
    // 1、在授权页面点击"授权"后，调用post请求
    // 2、第三方软件通过code请求token
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        System.out.println("start accept post req, generate access_toen");
        String reqType = request.getParameter("reqType");

        String grantType = request.getParameter("grant_type");
        String appId = request.getParameter("app_id");
        String appSecret = request.getParameter("app_secret");

        String responseType = request.getParameter("response_type");
        String redirectUri =request.getParameter("redirect_uri");
        String scope =request.getParameter("scope");

        // 处理用户点击approve按钮动作
        if("approve".equals(reqType)){
            String reqid = request.getParameter("reqid");//假设一定能够获取到值
            if(!reqidMap.containsKey(reqid)){
                return;
            }
            if("code".equals(responseType)){
                String[] rscope =request.getParameterValues("rscope");
                // 验证权限范围，这里进行第二次验证
                if(!checkScope(rscope)){
                    // 超出注册的权限范围
                    System.out.println("out of scope ...");
                    return;
                }
                // 要生成授权码，用户一定先登录授权服务；模拟登陆用户为USERTEST
                String code = generateCode(appId,"USERTEST");
                // 授权范围与授权码做绑定
                codeScopeMap.put(code,rscope);

                Map<String, String> params = new HashMap<String, String>();
                params.put("code",code);
                // 构造第三方软件的回调地址，并重定向到该地址
                String toAppUrl = URLParamsUtil.appendParams(redirectUri,params);
                // 授权码流程的【第二次】重定向
                response.sendRedirect(toAppUrl);
            }
        }

        //处理授权码流程中的 颁发访问令牌 环节
        if("authorization_code".equals(grantType)){
            // 验证第三方软件请求的参数是否与当时注册的参数一致
            if(!appMap.get("app_id").equals(appId)){
                response.getWriter().write("app_id is not available");
                return;
            }
            if(!appMap.get("app_secret").equals(appSecret)){
                response.getWriter().write("app_secret is not available");
                return;
            }

            String code = request.getParameter("code");
            // 验证code值，这里只是判断了code值是否存在，实际上应该取出code值对应的数据，判断appId是否一致，user是否一致，是否过期等
            if(!isExistCode(code)){
                return;
            }
            codeMap.remove(code);//授权码一旦被使用，须要立即作废

            System.out.println("start generate access_toen");
            String accessToken = generateAccessToken(appId,"USERTEST");//生成访问令牌access_token的值
            tokenScopeMap.put(accessToken,codeScopeMap.get(code));//授权范围与访问令牌绑定

            String refreshToken = generateRefreshToken(appId,"USERTEST");//生成刷新令牌refresh_token的值

            // TODO: 2020/2/28 将accessToken和refreshToken做绑定 ，将refreshToken和codeScopeMap做绑定

            response.getWriter().write(accessToken+"|"+refreshToken);

        }else if("refresh_token".equals(grantType)){// 处理刷新令牌请求环节
            /*
            在 OAuth 2.0 规范中，刷新令牌是一种特殊的授权许可类型，是嵌入在授权码许可类型下的一种特殊许可类型。
             */
            if(!"APPIDTEST".equals(appId)){
                response.getWriter().write("app_id is not available");
                return;
            }
            if(!"APPSECRETTEST".equals(appSecret)){
                response.getWriter().write("app_secret is not available");
                return;
            }

            String refresh_token = request.getParameter("refresh_token");
            // 判断refresh_token值是否存在
            if(!refreshTokenMap.containsKey(refresh_token)){
                return;
            }

            String appStr = refreshTokenMap.get(refresh_token);
            // 这里只是判断了appId是否一致，实际上应该取出refresh_token值对应的数据，判断appId是否一致，user是否一致，是否过期等
            if(!appStr.startsWith(appId+"|"+"USERTEST")){
                return;
            }
            // 当refresh_token校验成功后，生成访问令牌access_token的值
            String accessToken = generateAccessToken(appId,"USERTEST");

            // TODO: 2020/2/28 删除旧的access_token 、删除旧的refresh_token、生成新的refresh_token
            // 当第三方软件的访问令牌过期, 触发了刷新令牌的刷新, 重新生成访问令牌和刷新令牌, 这是过期时间又被重置。 如此循环是不是说, 永远不会过期啊 ??
            // 作者回答：refresh_token过期时间不会重新刷新

            // 为什么要 通过刷新令牌让第三方不断刷新token有效期，而不是直接给访问token一个更长的有效期？后者更简单
            // 作者回答：为了安全性的考虑，是不可以让“token一个更长的有效期”存在的。

            // 授权服务会将新的访问令牌和新的刷新令牌 accessToken+"|"+refreshToken，一起返回给第三方软件
            response.getWriter().write(accessToken);
        }
    }

    // 第三方软件第一次重定向到授权服务
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String responseType = request.getParameter("response_type");
        String redirectUri =request.getParameter("redirect_uri");
        String appId = request.getParameter("app_id");
        String scope = request.getParameter("scope");

        System.out.println("8081 GET responseType: "+responseType);
        // 验证第三方软件请求的参数是否与当时注册的参数一致
        if(!appMap.get("app_id").equals(appId)){
            return;
        }
        if(!appMap.get("redirect_uri").equals(redirectUri)){
            return;
        }
        // 验证第三方软件请求的权限范围是否与当时注册的权限范围一致
        if(!checkScope(scope)){
            // 超出注册的权限范围
            return;
        }
        // 生成页面reqid
        String reqid = String.valueOf(System.currentTimeMillis());
        reqidMap.put(reqid,reqid);// 保存该reqid值

        request.setAttribute("reqid",reqid);
        request.setAttribute("response_type",responseType);
        request.setAttribute("redirect_uri",redirectUri);
        request.setAttribute("app_id",appId);

        // 跳转到授权页面，再次选择授权范围，然后点击"授权"，调用post请求
        request.getRequestDispatcher("/approve.jsp").forward(request,response);

        //至此颁发授权码code的准备工作完毕
    }


    /**
     * 生成code值
     * @return
     */
    private String generateCode(String appId,String user) {
        Random r = new Random();
        StringBuilder strb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            strb.append(r.nextInt(10));
        }

        String code = strb.toString();

        // 在这一篇章我们仅作为演示用，实际这应该是一个全局内存数据库，有效期官方建议是10分钟
        codeMap.put(code,appId+"|"+user+"|"+System.currentTimeMillis());

        return code;
    }


    /**
     * 生成access_token值
     * @param appId
     * @param user
     * @return
     */
    private String generateAccessToken(String appId,String user){

        String accessToken = UUID.randomUUID().toString();
        // 1天时间过期
        String expires_in = "1";
        // 在这一篇章我们仅作为演示用，实际这应该是一个全局数据库,并且有有效期
        tokenMap.put(accessToken,appId+"|"+user+"|"+System.currentTimeMillis()+"|"+expires_in);

        return accessToken;
    }


    /**
     * 生成refresh_token值
     * @param appId
     * @param user
     * @return
     */
    private String generateRefreshToken(String appId,String user){

        String refreshToken = UUID.randomUUID().toString();

        //在这一篇章我们仅作为演示用，实际这应该是一个全局数据库,并且有有效期
        refreshTokenMap.put(refreshToken,appId+"|"+user+"|"+System.currentTimeMillis());

        return refreshToken;
    }

    /**
     * 是否存在code值
     * @param code
     * @return
     */
    private boolean isExistCode(String code){
        return codeMap.containsKey(code);
    }

    /**
     * 验证权限
     * @param scope
     * @return
     */
    private boolean checkScope(String scope){

        System.out.println("appMap size: "+appMap.size());
        System.out.println("appMap scope: "+appMap.get("scope"));
        System.out.println("scope: "+scope);

        return appMap.get("scope").contains(scope);//简单模拟权限验证
    }


    /**
     *
     * @param rscope
     * @return
     */
    private boolean checkScope(String[] rscope){
        String scope="";

        for(int i=0; i<rscope.length ;i++){
            scope=scope+rscope[i];
        }

        return appMap.get("scope").replace(" ","").contains(scope);//简单模拟权限验证
    }


    public static void main(String[] args) {

//        System.out.println(new OauthServlet().generateCode());
        System.out.println(UUID.randomUUID());

    }

}
