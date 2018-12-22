package cn.wellcare.admin.controller.system;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cn.wellcare.captcha.CommonCaptcha;

@RequestMapping("system")
@Controller
public class SystemController {

	@RequestMapping(value = "index")
	public String index(HttpServletRequest request, HttpServletResponse response,
			@RequestParam Map<String, Object> param) {
		return "index";
	}

	@RequestMapping({ "verifyCode" })
	public void verify(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		new CommonCaptcha().getCaptcha(request, response);
	}

	/**
	 * 访问无权限URL时跳转路径
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "unauth")
	public String unAuth() throws Exception {
		return "system/unauth";
	}
}
