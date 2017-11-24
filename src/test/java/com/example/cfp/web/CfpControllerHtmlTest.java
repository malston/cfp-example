package com.example.cfp.web;

import com.example.cfp.domain.Submission;
import com.example.cfp.domain.Track;
import com.example.cfp.domain.User;
import com.example.cfp.security.SecurityConfig;
import com.example.cfp.submission.SubmissionRequest;
import com.example.cfp.submission.SubmissionService;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebConnection;

import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@WebMvcTest(CfpController.class)
@Import(SecurityConfig.class)
public class CfpControllerHtmlTest {

    @Autowired
    private WebClient client;

    @MockBean
    private SubmissionService submissionService;

    @Autowired
    MockMvc mockMvc;

    @Before
    public void setup() {
        client.setWebConnection(new MockMvcWebConnection(mockMvc, client));
    }

    @Test
    public void submitTalk() throws IOException {
        SubmissionRequest request = new SubmissionRequest();
        request.setTitle("Alice in Wonderland");
        request.setSummary("my abstract");
        request.setTrack(Track.ALTERNATE_LANGUAGES);
        given(this.submissionService.create(request)).willReturn(new Submission());

        HtmlPage page = this.client.getPage("/submit");
        HtmlForm form = page.getForms().get(0);
        form.getInputByName("title").setValueAttribute(request.getTitle());
        form.getTextAreaByName("summary").setText(request.getSummary());
        form.getSelectByName("track").setSelectedAttribute(
                request.getTrack().getId(), true);

        HtmlButton submit = page.getFirstByXPath("//button[@type='submit']");
        submit.click();
        verify(this.submissionService).create(request);
    }

    @TestConfiguration
    @EnableConfigurationProperties(OAuth2ClientProperties.class)
    static class TestSecurityConfig {

        private final OAuth2ClientProperties credentials;

        public TestSecurityConfig(OAuth2ClientProperties credentials) {
            this.credentials = credentials;
        }

        @Bean
        public ResourceServerProperties resourceServerProperties() {
            return new ResourceServerProperties(this.credentials.getClientId(),
                    this.credentials.getClientSecret());
        }

        @Bean
        public AuthoritiesExtractor authoritiesExtractor() {
            return map -> {
                String username = (String) map.get("login");
                if ("jsmith".contains(username)) {
                    return AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER,ROLE_ADMIN");
                }
                else {
                    return AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER");
                }
            };
        }

        @Bean
        public PrincipalExtractor principalExtractor() {
            return map -> {
                User speaker = new User("jsmith@example.com", "John Smith");
                speaker.setGithub("jsmith");
                speaker.setAvatarUrl("https://acme.org/team/jsmith/avatar");
                return speaker;
            };
        }
    }

}
