package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.web.rest.TestUtil.sameInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Post;
import com.mycompany.myapp.repository.PostRepository;
import com.mycompany.myapp.service.dto.PostDTO;
import com.mycompany.myapp.service.mapper.PostMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;

/**
 * Integration tests for the {@link PostResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class PostResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENTS = "AAAAAAAAAA";
    private static final String UPDATED_CONTENTS = "BBBBBBBBBB";

    private static final Integer DEFAULT_READ_CNT = 1;
    private static final Integer UPDATED_READ_CNT = 2;

    private static final Integer DEFAULT_GOOD_CNT = 1;
    private static final Integer UPDATED_GOOD_CNT = 2;

    private static final ZonedDateTime DEFAULT_CREATED_AT = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_CREATED_AT = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final Long DEFAULT_CREATED_BY = 1L;
    private static final Long UPDATED_CREATED_BY = 2L;

    private static final String ENTITY_API_URL = "/api/posts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPostMockMvc;

    private Post post;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Post createEntity(EntityManager em) {
        Post post = new Post()
            .title(DEFAULT_TITLE)
            .contents(DEFAULT_CONTENTS)
            .readCnt(DEFAULT_READ_CNT)
            .goodCnt(DEFAULT_GOOD_CNT)
            .createdAt(DEFAULT_CREATED_AT)
            .createdBy(DEFAULT_CREATED_BY);
        return post;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Post createUpdatedEntity(EntityManager em) {
        Post post = new Post()
            .title(UPDATED_TITLE)
            .contents(UPDATED_CONTENTS)
            .readCnt(UPDATED_READ_CNT)
            .goodCnt(UPDATED_GOOD_CNT)
            .createdAt(UPDATED_CREATED_AT)
            .createdBy(UPDATED_CREATED_BY);
        return post;
    }

    @BeforeEach
    public void initTest() {
        post = createEntity(em);
    }

    @Test
    @Transactional
    void createPost() throws Exception {
        int databaseSizeBeforeCreate = postRepository.findAll().size();
        // Create the Post
        PostDTO postDTO = postMapper.toDto(post);
        restPostMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(postDTO)))
            .andExpect(status().isCreated());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeCreate + 1);
        Post testPost = postList.get(postList.size() - 1);
        assertThat(testPost.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testPost.getContents()).isEqualTo(DEFAULT_CONTENTS);
        assertThat(testPost.getReadCnt()).isEqualTo(DEFAULT_READ_CNT);
        assertThat(testPost.getGoodCnt()).isEqualTo(DEFAULT_GOOD_CNT);
        assertThat(testPost.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testPost.getCreatedBy()).isEqualTo(DEFAULT_CREATED_BY);
    }

    @Test
    @Transactional
    void createPostWithExistingId() throws Exception {
        // Create the Post with an existing ID
        post.setId(1L);
        PostDTO postDTO = postMapper.toDto(post);

        int databaseSizeBeforeCreate = postRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPostMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(postDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        int databaseSizeBeforeTest = postRepository.findAll().size();
        // set the field null
        post.setTitle(null);

        // Create the Post, which fails.
        PostDTO postDTO = postMapper.toDto(post);

        restPostMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(postDTO)))
            .andExpect(status().isBadRequest());

        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = postRepository.findAll().size();
        // set the field null
        post.setCreatedAt(null);

        // Create the Post, which fails.
        PostDTO postDTO = postMapper.toDto(post);

        restPostMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(postDTO)))
            .andExpect(status().isBadRequest());

        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedByIsRequired() throws Exception {
        int databaseSizeBeforeTest = postRepository.findAll().size();
        // set the field null
        post.setCreatedBy(null);

        // Create the Post, which fails.
        PostDTO postDTO = postMapper.toDto(post);

        restPostMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(postDTO)))
            .andExpect(status().isBadRequest());

        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPosts() throws Exception {
        // Initialize the database
        postRepository.saveAndFlush(post);

        // Get all the postList
        restPostMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(post.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].contents").value(hasItem(DEFAULT_CONTENTS.toString())))
            .andExpect(jsonPath("$.[*].readCnt").value(hasItem(DEFAULT_READ_CNT)))
            .andExpect(jsonPath("$.[*].goodCnt").value(hasItem(DEFAULT_GOOD_CNT)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(sameInstant(DEFAULT_CREATED_AT))))
            .andExpect(jsonPath("$.[*].createdBy").value(hasItem(DEFAULT_CREATED_BY.intValue())));
    }

    @Test
    @Transactional
    void getPost() throws Exception {
        // Initialize the database
        postRepository.saveAndFlush(post);

        // Get the post
        restPostMockMvc
            .perform(get(ENTITY_API_URL_ID, post.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(post.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.contents").value(DEFAULT_CONTENTS.toString()))
            .andExpect(jsonPath("$.readCnt").value(DEFAULT_READ_CNT))
            .andExpect(jsonPath("$.goodCnt").value(DEFAULT_GOOD_CNT))
            .andExpect(jsonPath("$.createdAt").value(sameInstant(DEFAULT_CREATED_AT)))
            .andExpect(jsonPath("$.createdBy").value(DEFAULT_CREATED_BY.intValue()));
    }

    @Test
    @Transactional
    void getNonExistingPost() throws Exception {
        // Get the post
        restPostMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewPost() throws Exception {
        // Initialize the database
        postRepository.saveAndFlush(post);

        int databaseSizeBeforeUpdate = postRepository.findAll().size();

        // Update the post
        Post updatedPost = postRepository.findById(post.getId()).get();
        // Disconnect from session so that the updates on updatedPost are not directly saved in db
        em.detach(updatedPost);
        updatedPost
            .title(UPDATED_TITLE)
            .contents(UPDATED_CONTENTS)
            .readCnt(UPDATED_READ_CNT)
            .goodCnt(UPDATED_GOOD_CNT)
            .createdAt(UPDATED_CREATED_AT)
            .createdBy(UPDATED_CREATED_BY);
        PostDTO postDTO = postMapper.toDto(updatedPost);

        restPostMockMvc
            .perform(
                put(ENTITY_API_URL_ID, postDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(postDTO))
            )
            .andExpect(status().isOk());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeUpdate);
        Post testPost = postList.get(postList.size() - 1);
        assertThat(testPost.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testPost.getContents()).isEqualTo(UPDATED_CONTENTS);
        assertThat(testPost.getReadCnt()).isEqualTo(UPDATED_READ_CNT);
        assertThat(testPost.getGoodCnt()).isEqualTo(UPDATED_GOOD_CNT);
        assertThat(testPost.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testPost.getCreatedBy()).isEqualTo(UPDATED_CREATED_BY);
    }

    @Test
    @Transactional
    void putNonExistingPost() throws Exception {
        int databaseSizeBeforeUpdate = postRepository.findAll().size();
        post.setId(count.incrementAndGet());

        // Create the Post
        PostDTO postDTO = postMapper.toDto(post);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPostMockMvc
            .perform(
                put(ENTITY_API_URL_ID, postDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(postDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPost() throws Exception {
        int databaseSizeBeforeUpdate = postRepository.findAll().size();
        post.setId(count.incrementAndGet());

        // Create the Post
        PostDTO postDTO = postMapper.toDto(post);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(postDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPost() throws Exception {
        int databaseSizeBeforeUpdate = postRepository.findAll().size();
        post.setId(count.incrementAndGet());

        // Create the Post
        PostDTO postDTO = postMapper.toDto(post);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(postDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePostWithPatch() throws Exception {
        // Initialize the database
        postRepository.saveAndFlush(post);

        int databaseSizeBeforeUpdate = postRepository.findAll().size();

        // Update the post using partial update
        Post partialUpdatedPost = new Post();
        partialUpdatedPost.setId(post.getId());

        partialUpdatedPost.goodCnt(UPDATED_GOOD_CNT).createdBy(UPDATED_CREATED_BY);

        restPostMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPost.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedPost))
            )
            .andExpect(status().isOk());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeUpdate);
        Post testPost = postList.get(postList.size() - 1);
        assertThat(testPost.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testPost.getContents()).isEqualTo(DEFAULT_CONTENTS);
        assertThat(testPost.getReadCnt()).isEqualTo(DEFAULT_READ_CNT);
        assertThat(testPost.getGoodCnt()).isEqualTo(UPDATED_GOOD_CNT);
        assertThat(testPost.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testPost.getCreatedBy()).isEqualTo(UPDATED_CREATED_BY);
    }

    @Test
    @Transactional
    void fullUpdatePostWithPatch() throws Exception {
        // Initialize the database
        postRepository.saveAndFlush(post);

        int databaseSizeBeforeUpdate = postRepository.findAll().size();

        // Update the post using partial update
        Post partialUpdatedPost = new Post();
        partialUpdatedPost.setId(post.getId());

        partialUpdatedPost
            .title(UPDATED_TITLE)
            .contents(UPDATED_CONTENTS)
            .readCnt(UPDATED_READ_CNT)
            .goodCnt(UPDATED_GOOD_CNT)
            .createdAt(UPDATED_CREATED_AT)
            .createdBy(UPDATED_CREATED_BY);

        restPostMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPost.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedPost))
            )
            .andExpect(status().isOk());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeUpdate);
        Post testPost = postList.get(postList.size() - 1);
        assertThat(testPost.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testPost.getContents()).isEqualTo(UPDATED_CONTENTS);
        assertThat(testPost.getReadCnt()).isEqualTo(UPDATED_READ_CNT);
        assertThat(testPost.getGoodCnt()).isEqualTo(UPDATED_GOOD_CNT);
        assertThat(testPost.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testPost.getCreatedBy()).isEqualTo(UPDATED_CREATED_BY);
    }

    @Test
    @Transactional
    void patchNonExistingPost() throws Exception {
        int databaseSizeBeforeUpdate = postRepository.findAll().size();
        post.setId(count.incrementAndGet());

        // Create the Post
        PostDTO postDTO = postMapper.toDto(post);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPostMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, postDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(postDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPost() throws Exception {
        int databaseSizeBeforeUpdate = postRepository.findAll().size();
        post.setId(count.incrementAndGet());

        // Create the Post
        PostDTO postDTO = postMapper.toDto(post);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(postDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPost() throws Exception {
        int databaseSizeBeforeUpdate = postRepository.findAll().size();
        post.setId(count.incrementAndGet());

        // Create the Post
        PostDTO postDTO = postMapper.toDto(post);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPostMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(postDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Post in the database
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePost() throws Exception {
        // Initialize the database
        postRepository.saveAndFlush(post);

        int databaseSizeBeforeDelete = postRepository.findAll().size();

        // Delete the post
        restPostMockMvc
            .perform(delete(ENTITY_API_URL_ID, post.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Post> postList = postRepository.findAll();
        assertThat(postList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
