/**
 * Copyright (c) 2015-2016 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Bosch Software Innovations GmbH - Please refer to git log
 */
package org.eclipse.vorto.repository.web;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.vorto.repository.account.IUserAccountService;
import org.eclipse.vorto.repository.account.impl.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
public class HomeController {
	
	private static final String LOGIN_TYPE = "loginType";
	private static final String LOGOUT_URL = "/logout";

	@Value("${github.oauth2.enabled}")
	private boolean githubEnabled;
	
	@Value("${eidp.oauth2.enabled}")
	private boolean eidpEnabled;
	
	@Value("${server.config.authenticatedSearchMode:#{false}}")
	private boolean authenticatedSearchMode = false;

    @Value("${eidp.oauth2.resource.logoutEndpointUrl:#{null}}")
    private String logoutEndpointUrl;

    @Value("${eidp.oauth2.resource.logoutRedirectUrl:#{null}}")
    private String logoutRedirectUrl;

    @Value("#{servletContext.contextPath}")
    private String servletContextPath;

    @Value("${repo.attachment.allowed.fileSize}")
    private String attachmentAllowedSize;

    @Autowired
    private OAuth2ClientContext oauth2ClientContext;
	
	@Value("${server.config.updateDate:#{'2000-01-01 12:00:00'}}")
	private String updateDate;
	
	@Value("${server.config.supportEmail:#{null}}")
	private String supportEmail;
	
	@Autowired
	private IUserAccountService accountService;
	
	@SuppressWarnings("unchecked")
	@ApiOperation(value = "Returns the currently logged in User")
	@ApiResponses(value = { @ApiResponse(code = 401, message = "Unauthorized"), 
							@ApiResponse(code = 200, message = "OK")})
	@RequestMapping(value ={ "/user", "/me" }, method = RequestMethod.GET)
	public ResponseEntity<Map<String, String>> getUser(Principal user, final HttpServletRequest request) throws ParseException {
		
		Map<String, String> map = new LinkedHashMap<>();

		if(user == null)
			return new ResponseEntity<Map<String, String>>(map, HttpStatus.UNAUTHORIZED);
		
		OAuth2Authentication oauth2User = (OAuth2Authentication) user;
		
		oauth2User.getAuthorities().stream().findFirst().ifPresent(role -> map.put("role", role.getAuthority()));
		
		User userAccount = accountService.getUser(oauth2User.getName());
		
		Date updateCutoff = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(updateDate);
		
		map.put("name", oauth2User.getName());
		map.put("displayName", getDisplayName(oauth2User));
		map.put("isRegistered", Boolean.toString(userAccount != null));
		map.put("needUpdate", Boolean.toString(needUpdate(userAccount, updateCutoff)));
		Map<String, String> userDetails = ((Map<String, String>) oauth2User.getUserAuthentication().getDetails());
		map.put("loginType", userDetails.get(LOGIN_TYPE));
		
		return new ResponseEntity<Map<String, String>>(map, HttpStatus.OK);
	}

	private boolean needUpdate(User user, Date updateCutoff) {
		return user != null && user.getLastUpdated().before(updateCutoff) && new Date().after(updateCutoff);
	}
	
	private String getDisplayName(OAuth2Authentication oauth2User) {
		UsernamePasswordAuthenticationToken userAuth = (UsernamePasswordAuthenticationToken) oauth2User.getUserAuthentication();
		
		@SuppressWarnings("unchecked")
		Map<String, Object> userDetailsMap = (Map<String, Object>) userAuth.getDetails();
		
		String login = (String) userDetailsMap.get("login");
		if (login != null) {
			return login;
		}
		
		String email = (String) userDetailsMap.get("email");
		if (email != null) {
			return email.split("@")[0];
		}
		
		return oauth2User.getName();
	}

	@RequestMapping(value ={ "/context" }, method = RequestMethod.GET)
	public Map<String, Object> globalContext(final HttpServletRequest request) {
		Map<String, Object> context = new LinkedHashMap<>();
		
		context.put("githubEnabled", githubEnabled);
		context.put("eidpEnabled", eidpEnabled);
		context.put("authenticatedSearchMode", authenticatedSearchMode);
		context.put("logOutUrl", getLogoutEndpointUrl(getBaseUrl(request)));
		context.put("attachmentAllowedSize", attachmentAllowedSize);
		context.put("supportEmail", supportEmail);
		
		return context;
	}

    public String getBaseUrl(HttpServletRequest request) {
        if (request.getRequestURI().equals("/")
                || request.getRequestURI().equals("")) {
            return request.getRequestURL().toString();
        }else {
            return request.getRequestURL().toString()
                    .replace(request.getRequestURI(), "");
        }

    }

    private String getLogoutEndpointUrl(String baseUrl) {
	    if(eidpEnabled) {
            String idToken = "";
            if(SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2Authentication) {
                idToken = (String) oauth2ClientContext.getAccessToken().getAdditionalInformation().get("id_token");
            }
            return String.format("%s?id_token_hint=%s&post_logout_redirect_uri=%s", logoutEndpointUrl, idToken, logoutRedirectUrl);
        }else {
            return baseUrl + servletContextPath + LOGOUT_URL;
        }
    }
	
}
