package com.oauth.ch06;

import com.my.util.URLParamsUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * **
 * 使用此类来模拟【第三方软件的首页】
 */
@WebServlet("/AppIndexServlet-ch02")
public class AppIndexServlet extends HttpServlet {

    String oauthUrl = "http://localhost:8081/OauthServlet-ch02";
    String redirectUrl = "http://localhost:8080/AppServlet-ch02";


    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        //授权码许可流程，DEMO CODE
        /*System.out.println("app index ...");
        Map<String, String> params = new HashMap<String, String>();
        params.put("response_type","code");
        params.put("redirect_uri","http://localhost:8080/AppServlet-ch02");
        params.put("app_id","APPIDTEST");


        String toOauthUrl = URLParamsUtil.appendParams(oauthUrl,params);//构造请求授权的URl

        System.out.println("toOauthUrl: "+toOauthUrl);

        response.sendRedirect(toOauthUrl);//授权码流程的第一次重定向

        //response.sendRedirect("http://localhost:8081/OauthServlet-ch02?response_type=code&redirect_uri=http://localhost:8080/AppServlet-ch02&app_id=APPIDTEST");
*/
        //隐式许可流程（模拟），DEMO CODE
        // 第一步还是小明访问第三方软件的首页，然后第三方软件由于没有后端服务，所以直接重定向到授权服务
        Map<String, String> params = new HashMap<String, String>();
        params.put("response_type","token");//告诉授权服务直接返回access_token
        params.put("redirect_uri","http://localhost:8080/AppServlet-ch02");
        params.put("app_id","APPIDTEST");

        String toOauthUrl = URLParamsUtil.appendParams(oauthUrl,params);//构造请求授权的URl

        response.sendRedirect(toOauthUrl);

    }
}
