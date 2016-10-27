package com.example.cfp.web;

import com.example.cfp.domain.Submission;
import com.example.cfp.domain.Track;
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
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebConnection;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@WebMvcTest(CfpController.class)
public class CfpControllerHtmlTest {

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private WebClient client;

    @MockBean
    private SubmissionService submissionService;

    @Autowired
    private WebApplicationContext context;

    @Before
    public void setup() {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
        client.setWebConnection(new MockMvcWebConnection(mockMvc, client));
        client.getCookieManager().clearCookies();
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
    @EnableOAuth2Sso
    static class TestSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .antMatchers("/admin/**").hasRole("ADMIN")
                    .antMatchers("/", "/news", "/submit", "/login**", "/css/**", "/img/**", "/webjars/**", "/bootstrap/**").permitAll()
                    .anyRequest().authenticated()
                    .and()
                .csrf()
                    .ignoringAntMatchers("/admin/h2-console/*")
                    .and()
                .logout()
                    .logoutSuccessUrl("/")
                    .permitAll()
                    .and()
                .headers()
                    .frameOptions().sameOrigin();
        }

        @Bean
        public ResourceServerProperties resourceServerProperties() {
            return new ResourceServerProperties();
        }
    }


}
