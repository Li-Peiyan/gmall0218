package com.atguigu.gmall0218.gmalllistservice;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@SpringBootTest
class GmallListServiceApplicationTests {

    @Test
    void contextLoads() {
    }

//    @Autowired
//    private JestClient jestClient;

    @Test
    public void testEs() throws IOException {
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder("http://81.69.33.96:9200")
                .multiThreaded(true)
                .build());
        JestClient jestClient = factory.getObject();




        String query="{\n" +
                "  \"query\": {\n" +
                "    \"match\": {\n" +
                "      \"actorList.name\": \"张译\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();

        SearchResult result = jestClient.execute(search);

        List<SearchResult.Hit<HashMap, Void>> hits = result.getHits(HashMap.class);

        for (SearchResult.Hit<HashMap, Void> hit : hits) {
            HashMap source = hit.source;
            System.err.println("source = " + source);
        }

    }


}
