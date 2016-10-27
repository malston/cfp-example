package com.example.cfp.web;

import com.example.cfp.domain.SubmissionRepository;
import com.example.cfp.domain.Track;
import com.example.cfp.domain.User;
import com.example.cfp.domain.UserRepository;
import com.example.cfp.submission.SubmissionRequest;
import com.gargoylesoftware.htmlunit.WebClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebConnection;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CfpControllerIntegrationTest {

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private WebClient client;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext context;

    @Before
    public void setup() throws Exception {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
				.addFilters(springSecurityFilterChain)
                .build();
        client.setWebConnection(new MockMvcWebConnection(mvc, client));
        client.getCookieManager().clearCookies();
    }

    @Test
    public void submitTalk() throws Exception {
        SubmissionRequest request = new SubmissionRequest();
        User speaker = new User("jsmith@example.com", "John Smith");
        speaker = this.userRepository.save(speaker);
        request.setTitle("Alice in Wonderland");
        request.setSummary("my abstract");
        request.setTrack(Track.ALTERNATE_LANGUAGES);
        request.setSpeaker(speaker);
        this.mvc.perform(post("/submit")
                .param("email", request.getSpeaker().getEmail())
                .param("name", request.getSpeaker().getName())
                .param("title", request.getTitle())
                .param("summary", request.getSummary())
                .param("track", request.getTrack().getId())
                .with(authentication(new TestingAuthenticationToken(
                        speaker, "secret", "ROLE_USER")))
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/submit?navSection=submit"));
        assertThat(this.submissionRepository.findBySpeaker(speaker)).hasSize(1);
    }

}
