package cn.wellcare.portal.controller.demo;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.wellcare.core.constant.BaseParam;
import cn.wellcare.core.constant.Constants;
import cn.wellcare.core.constant.PaymentType;
import cn.wellcare.core.exception.BusinessException;
import cn.wellcare.core.exception.ErrorEnum;
import cn.wellcare.core.exception.UnauthorizedException;
import cn.wellcare.core.utils.CommonUtils;
import cn.wellcare.core.utils.Md5SignUtil;
import cn.wellcare.payment.api.RefundPayApi;
import cn.wellcare.payment.modules.system.ISysOrganizationService;
import cn.wellcare.pojo.common.RefundPayResult;
import cn.wellcare.pojo.common.RpcResult;

@RequestMapping("demo/refundPayAction")
@Controller
public class DemoRefundPayController {
	protected Logger log = Logger.getLogger(this.getClass());

	@Resource
	private ISysOrganizationService sysOrganizationService;

	@Resource(name = "wxNativeRefundPayService")
	private RefundPayApi wxNativeRefundPayService;

	@Resource(name = "integrationRefundPayService")
	private RefundPayApi integrationRefundPayService;

	@Resource(name = "alipayRefundService")
	private RefundPayApi alipayRefundService;

	@RequestMapping(value = Constants.REFUNDPAY, produces = Constants.CONTENT_TYPE_JSON)
	@ResponseBody
	public RpcResult<RefundPayResult> demoRefundPay(HttpServletRequest request, HttpServletResponse response,
			@RequestParam Map<String, Object> param) {
		RpcResult<RefundPayResult> refundPayResult = new RpcResult<>();
		try {

			Assert.notNull(param.get(BaseParam.OUT_TRADE_NO));// 退款订单号
			Assert.notNull(param.get(BaseParam.REFUND_AMOUNT));// 退款金额

			Map<String, Object> result = CommonUtils.sort(param);
			log.debug("客户端排序后参数集：" + result);

			String securtkey = sysOrganizationService
					.getSysOrganizationById(Integer.valueOf((String) param.get(BaseParam.ORG_ID)))
					.getData().getAuthSecret();
			log.debug("机构密钥：" + securtkey);

			String sign = Md5SignUtil.sginMD5(result, securtkey);
			param.put("sign", sign);

			String payType = (String) param.get(BaseParam.PAY_TYPE);
			Assert.notNull(payType);

			if (PaymentType.WECHAT_NATIVE.getPaymentCode().equals(payType)
					|| PaymentType.WECHAT_JSAPI.getPaymentCode().equals(payType)
					|| PaymentType.WECHAT_SAOMA.getPaymentCode().equals(payType)
					|| PaymentType.WECHAT_NATIVE.getPaymentName().equals(payType)
					|| PaymentType.WECHAT_JSAPI.getPaymentName().equals(payType)
					|| PaymentType.ALIPAY_SAOMA.getPaymentName().equals(payType)) {
				// 微信本地扫码支付
				refundPayResult = wxNativeRefundPayService.refundPay(param);
			} else if (PaymentType.JUHPAY.getPaymentCode().equals(payType)
					|| PaymentType.JUHPAY.getPaymentName().equals(payType)) {
				refundPayResult = integrationRefundPayService.refundPay(param);
			} else if (PaymentType.ALIPAY.getPaymentCode().equals(payType)
					|| PaymentType.ALIPAY_SAOMA.getPaymentCode().equals(payType)
					|| PaymentType.ALIPAY.getPaymentName().equals(payType)
					|| PaymentType.ALIPAY_SAOMA.getPaymentName().equals(payType)) {
				refundPayResult = alipayRefundService.refundPay(param);
			}
		} catch (Exception e) {
			refundPayResult.setSuccess(false);
			if (e instanceof BusinessException) {
				BusinessException pe = (BusinessException) e;
				if (pe.getCode() != null && ErrorEnum.SERVER_EXCEPTION.getErrCode().equals(pe.getCode())) {
					refundPayResult.setMsgInfo(ErrorEnum.SERVER_EXCEPTION.getErrDesc());
				} else {
					refundPayResult.setMsgInfo(e.getMessage());
				}
			} else {
				if (e instanceof UnauthorizedException) {
					refundPayResult.setError(ErrorEnum.UNAUTHORIZED_EXCEPTION);
				} else {
					e.printStackTrace();
					refundPayResult.setMsgInfo(ErrorEnum.SERVER_EXCEPTION.getErrDesc());
				}
			}
		}
		return refundPayResult;
	}
}
