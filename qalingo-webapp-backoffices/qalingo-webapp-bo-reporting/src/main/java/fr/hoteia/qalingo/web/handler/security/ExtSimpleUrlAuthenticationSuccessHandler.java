/**
 * Most of the code in the Qalingo project is copyrighted Hoteia and licensed
 * under the Apache License Version 2.0 (release version 0.7.0)
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *                   Copyright (c) Hoteia, 2012-2013
 * http://www.hoteia.com - http://twitter.com/hoteia - contact@hoteia.com
 *
 */
package fr.hoteia.qalingo.web.handler.security;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import fr.hoteia.qalingo.core.common.domain.User;
import fr.hoteia.qalingo.core.common.domain.UserConnectionLog;
import fr.hoteia.qalingo.core.common.service.UserConnectionLogService;
import fr.hoteia.qalingo.core.common.service.UserService;
import fr.hoteia.qalingo.core.web.util.RequestUtil;
import fr.hoteia.qalingo.web.service.BoReportingUrlService;

@Component
public class ExtSimpleUrlAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final Logger LOG = LoggerFactory.getLogger(getClass());
	
    @Autowired
    private UserService userService;

    @Autowired
    private UserConnectionLogService userConnectionLogService;
    
	@Autowired
    protected RequestUtil requestUtil;
	
	@Autowired
    protected BoReportingUrlService boReportingUrlService;
	
	@Autowired
    protected ExtRedirectStrategy redirectStrategy;
	
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {

    	// Find the current user
    	User user = userService.getUserByLoginOrEmail(authentication.getName());

    	// Persit only the new UserConnectionLog
    	UserConnectionLog userConnectionLog = new UserConnectionLog();
    	userConnectionLog.setUserId(user.getId());
    	userConnectionLog.setLoginDate(new Date());
    	userConnectionLogService.saveOrUpdateUserConnectionLog(userConnectionLog);

		try {
	    	// Update the User in Session
	    	user.getConnectionLogs().add(userConnectionLog);
	    	requestUtil.updateCurrentUser(request, user);
	    	setUseReferer(false);
			String url = requestUtil.getCurrentRequestUrlNotSecurity(request);
			
	        // SANITY CHECK
	        if(StringUtils.isEmpty(url)){
	    		url = boReportingUrlService.buildHomeUrl(request);
	        }
	        
	    	setDefaultTargetUrl(url);
	        redirectStrategy.sendRedirect(request, response, url);
	        
		} catch (Exception e) {
			LOG.error("", e);
		}

    }
}
