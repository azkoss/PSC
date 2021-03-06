package cn.wellcare.api.ccp;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import cn.wellcare.constant.Constants;
import cn.wellcare.entity.ccp.ServiceConfig;
import cn.wellcare.entity.ccp.ServiceRegister;
import cn.wellcare.exception.BusinessException;
import cn.wellcare.exception.ErrorEnum;
import cn.wellcare.pojo.ServiceResponse;
import cn.wellcare.pojo.ServiceResult;
import cn.wellcare.service.ccp.ServiceConfigService;
import cn.wellcare.service.ccp.ServiceRegisterService;
import cn.wellcare.system.CodeManager;
import cn.wellcare.utils.CommonUtils;
import cn.wellcare.utils.HttpClientUtil;
import cn.wellcare.utils.JsonUtil;

@Controller
@RequestMapping(value = Constants.API_CCP)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CCPApi {

	@Resource
	private ServiceRegisterService serviceRegisterService;
	@Resource
	private ServiceConfigService serviceConfigService;

	@RequestMapping(value = "handle")
	public void handle(@RequestParam(required = true) String transCode, @RequestParam(required = false) String param,
			HttpServletRequest request, HttpServletResponse response) {
		ServiceRegister reg = null;
		String serviceResponse = null;
		try {
			reg = serviceRegisterService.getByTranscode(transCode);
			// 1.目标服务地址
			// TODO 来自服务配置
			ServiceResponse<ServiceConfig> configres = serviceConfigService
					.getServiceConfigServerCode(reg.getServerCode());
			if (!configres.getSuccess()) {
				throw new BusinessException("服务号不存在");
			}
			//String url = configres.getData().getAddr();
			String url = "http://localhost";
			// 2.目标端口
			int port = reg.getRequestPort();
			// 3.目标域
			String domain = reg.getRequestDomain();
			// 4.目标方法
			String method = reg.getRequestMethod();
			// 5.执行
			String reqtype = CodeManager.getCodeText(Constants.REQUEST_TYPE, reg.getRequestType());
			// 拼接rest地址
			url = url + ":" + port + "/" + domain + "/" + method;
			// 参数转换
			Map<String, Object> parammap = null;
			if (!CommonUtils.isNull(param)) {
				parammap = new Gson().fromJson(param, new TypeToken<Map<String, Object>>() {
				}.getType());
			}

			Integer timeout = reg.getTimeOut();
			switch (reqtype) {
			case "GET":
				serviceResponse = HttpClientUtil.doGet(url + "?" + CommonUtils.map2paramstring(parammap, true),
						timeout);
				break;
			case "POST":
				serviceResponse = HttpClientUtil.doPost(url, parammap, timeout);
				break;
			case "SOAP":
				// TODO
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 6.处理结果
		String datatype = CodeManager.getCodeText(Constants.DATA_TYPE, reg.getDataType());
		switch (datatype) {
		case "JSON":
			response.setContentType("application/json;charset=UTF-8");
			ServiceResult<Object> result = new ServiceResult<>();
			try {
				if (CommonUtils.isNull(serviceResponse)) {
					throw new BusinessException(ErrorEnum.SERVER_NO_RESPONSE.getErrDesc());
				}
				result = new Gson().fromJson(serviceResponse, new TypeToken<ServiceResult<Object>>() {
				}.getType());

			} catch (Exception e) {
				result.setStatus(Constants.SERVICE_RESULT_OTHER);
				if (e instanceof IOException) {
					result.setErrorMessage(e.getMessage());
				} else {
					e.printStackTrace();
					result.setErrorMessage(ErrorEnum.SERVER_EXCEPTION.getErrDesc());
				}
			}
			try {
				response.getWriter().write(JsonUtil.writeValueAsString(result));
			} catch (IOException e) {
				e.printStackTrace();
			}
		case "XML":
			// TODO
			break;
		default:
			break;
		}
	}

}
