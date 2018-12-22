package cn.wellcare.aop.authentication;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cn.wellcare.core.constant.BaseParam;
import cn.wellcare.core.constant.Constants;
import cn.wellcare.core.exception.BusinessException;
import cn.wellcare.core.exception.ErrorEnum;
import cn.wellcare.core.utils.CommonUtils;
import cn.wellcare.core.utils.Md5SignUtil;
import cn.wellcare.model.base.OrderModel;
import cn.wellcare.model.modules.system.SysOrganizationModel;

/**
 * 支付中心鉴权切面
 * 
 * @author zhaihl
 *
 */
@Component
@Aspect
public class PaymentAuthenticationAspect {
	private Logger log = Logger.getLogger(this.getClass());
	@Resource
	private OrderModel orderModel;
	@Resource
	private SysOrganizationModel sysOrganizationModel;

	@Pointcut("execution(* cn.wellcare.service.transaction.payment..*Service.*(..))")
	public void auth() {
	}

	@Before("auth() && args(param))")
	public void doAuth(Map<String, Object> param) throws Exception {
		this.log.debug("服务鉴权开始...");
		try {
			// 1.鉴权所需参数
			String orgId = (String) param.get(BaseParam.ORG_ID);
			String payType = (String) param.get(BaseParam.PAY_TYPE);
			String userId = param.get(BaseParam.USER_ID).toString();
			String payAmount = (String) param.get(BaseParam.PAY_AMOUNT);
			String clientSign = (String) param.get(BaseParam.DATA_SIGN);
			String encryptUser = (String) param.get(BaseParam.ENCRYPT_USER);
			String encryptPwd = (String) param.get(BaseParam.ENCRYPT_PWD);
			// 客户端时间
			String tradeTime = (String) param.get(BaseParam.CLIENT_TRADE_TIME);
			String clientIP = (String) param.get(BaseParam.CLIENT_IP);

			Assert.notNull(orgId, "机构号不能为空");
			Assert.notNull(payType, "支付方式不能为空");
			Assert.notNull(userId, "用户id不能为空");
			Assert.notNull(payAmount, "支付金额不能为空");
			Assert.notNull(clientSign, "客户端签名不能为空");
			Assert.notNull(encryptUser, "认证用户不能为空");
			Assert.notNull(encryptPwd, "认证密码不能为空");
			Assert.notNull(tradeTime, "交易时间不能为空");

			log.debug("----------金额处理 ----------");
			log.debug("支付金额：" + payAmount);
			DecimalFormat df = new DecimalFormat("#########0.00");
			String serverAmount = df.format(new BigDecimal(payAmount));
			param.put(BaseParam.PAY_AMOUNT, serverAmount);
			log.debug("格式化后值：" + serverAmount);

			// TODO 校验用户名和密码

			// 移除签名后排序
			param.remove(BaseParam.DATA_SIGN);
			param.remove(BaseParam.CLIENT_IP);

			// 将参数按key排序
			Map<String, Object> result = CommonUtils.sort(param);

			this.log.debug("排序后参数集：" + result);
			// TODO 以机构id获取其认证密钥
			String authEntrypt = sysOrganizationModel.getSysOrganizationById(Integer.valueOf(orgId)).getAuthSecret();

			// 2.服务器签名
			String serverEncrypt = Md5SignUtil.sginMD5(result, authEntrypt);
			log.debug("服务器签名：[" + serverEncrypt + "]，客户端签名：[" + clientSign + "]");
			// 3.和客户端签名比对
			if (serverEncrypt == null || !serverEncrypt.equals(clientSign)) {
				// TODO
				// throw new
				// UnauthorizedException(ErrorEnum.UNAUTHORIZED_EXCEPTION.getErrDesc());
				log.error("签名错误！！！");
			}
			// 4.时间比对
			long now = new Date().getTime();
			long diff = now - Long.valueOf(tradeTime);
			long seconds = diff / 1000;
			if (seconds > Constants.SERVER_ACCEPT_MAX_SECOND) {
				throw new BusinessException("时间不合法，请重新发起支付请求");
			}
			// 放入请求ip
			param.put(BaseParam.CLIENT_IP, clientIP);

			this.log.debug("服务鉴权完毕，没有异常...");
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorEnum.PARAM_IS_INVALID.getErrDesc());
		} catch (Exception e) {
			throw e;
		}
	}

	@AfterThrowing(value = "auth()", throwing = "e")
	public void afterThrowing(JoinPoint joinPoint, Exception e) throws Exception {
		this.log.debug("支付服务调用异常，异常信息：" + e.getMessage());
		throw e;
	}

}
