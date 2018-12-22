package cn.wellcare.admin.controller.system;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.wellcare.core.constant.Constants;
import cn.wellcare.core.exception.BusinessException;
import cn.wellcare.core.utils.HttpJsonResult;
import cn.wellcare.core.utils.PagerInfo;
import cn.wellcare.core.utils.WebUtil;
import cn.wellcare.entity.system.SysOrganization;
import cn.wellcare.payment.modules.system.ISysOrganizationService;
import cn.wellcare.pojo.common.RpcResult;

/**
 * 机构相关action
 * 
 * @Filename: SysOrganization.java
 * @Version: 1.0
 * 
 */
@Controller
@RequestMapping(value = "sysorganization")
public class SysOrganizationController {
	@Resource
	private ISysOrganizationService sysOrganizationService;

	/**
	 * 默认页面
	 * 
	 * @param dataMap
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "", method = { RequestMethod.GET })
	public String index(HttpServletRequest request, ModelMap dataMap) throws Exception {
		dataMap.put("pageSize", Constants.DEFAULT_PAGE_SIZE);

		Map<String, Object> queryMap = WebUtil.handlerQueryMap(request);
		dataMap.put("queryMap", queryMap);
		return "system/sysorganizationlist";
	}

	/**
	 * gridDatalist数据
	 * 
	 * @param request
	 * @param dataMap
	 * @return
	 */
	@RequestMapping(value = "list", method = { RequestMethod.GET })
	public @ResponseBody HttpJsonResult<List<SysOrganization>> list(HttpServletRequest request, ModelMap dataMap) {
		Map<String, Object> queryMap = WebUtil.handlerQueryMap(request);
		PagerInfo pager = WebUtil.handlerPagerInfo(request, dataMap);
		RpcResult<List<SysOrganization>> serviceResult = sysOrganizationService.page(queryMap, pager);
		if (!serviceResult.isSuccess()) {
			if (Constants.SERVICE_RESULT_CODE_SYSERROR.equals(serviceResult.getMsgCode())) {
				throw new RuntimeException(serviceResult.getMsgInfo());
			} else {
				throw new BusinessException(serviceResult.getMsgInfo());
			}
		}

		HttpJsonResult<List<SysOrganization>> jsonResult = new HttpJsonResult<List<SysOrganization>>();
		jsonResult.setRows(serviceResult.getData());
		jsonResult.setTotal(pager.getRowsCount());

		return jsonResult;
	}

	@RequestMapping(value = "add", method = { RequestMethod.GET })
	public String add(HttpServletRequest request, ModelMap dataMap, Integer id) {
		return "system/sysorganizationadd";
	}

	@RequestMapping(value = "edit", method = { RequestMethod.GET })
	public String edit(HttpServletRequest request, ModelMap dataMap, Integer id) {
		if (id != null) {
			SysOrganization sysOrganization = sysOrganizationService.getSysOrganizationById(id).getData();
			dataMap.put("obj", sysOrganization);
		}
		return "system/sysorganizationedit";
	}

	/**
	 * 新增/编辑
	 * 
	 * @param request
	 * @param response
	 * @param cate
	 */
	@RequestMapping(value = "doAdd", method = { RequestMethod.POST })
	public @ResponseBody HttpJsonResult<Boolean> doAdd(HttpServletRequest request, HttpServletResponse response,
			SysOrganization sysOrganization) {
		HttpJsonResult<Boolean> jsonResult = new HttpJsonResult<Boolean>();
		RpcResult<Integer> serviceResult = null;
		try {
			if (sysOrganization.getId() != null && 0 != sysOrganization.getId()) {
				serviceResult = sysOrganizationService.updateSysOrganization(sysOrganization);
			} else {
				serviceResult = sysOrganizationService.saveSysOrganization(sysOrganization);
			}
			jsonResult.setRows(serviceResult.getData() > 0);
		} catch (Exception e) {
			jsonResult.setMessage(e.getMessage());
			e.printStackTrace();
		}
		return jsonResult;
	}

	/**
	 * 删除
	 * 
	 * @param request
	 * @param response
	 * @param cate
	 */
	@RequestMapping(value = "del", method = { RequestMethod.GET })
	public @ResponseBody HttpJsonResult<Boolean> del(HttpServletRequest request, HttpServletResponse response,
			Integer id) {
		HttpJsonResult<Boolean> jsonResult = new HttpJsonResult<Boolean>();
		try {
			RpcResult<Boolean> serviceResult = sysOrganizationService.del(id);
			if (!serviceResult.isSuccess()) {
				if (Constants.SERVICE_RESULT_CODE_SYSERROR.equals(serviceResult.getMsgCode())) {
					throw new RuntimeException(serviceResult.getMsgInfo());
				} else {
					throw new BusinessException(serviceResult.getMsgInfo());
				}
			}
		} catch (Exception e) {
			jsonResult.setMessage(e.getMessage());
			e.printStackTrace();
		}
		return jsonResult;
	}

}
