package vn.nhom15.preorder.menu;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import vn.nhom15.preorder.support.TestDatabase;

/** Xem menu qua toàn bộ ứng dụng thật và PostgreSQL thật, khi chưa đăng nhập (BR-34, UC-03). */
@SpringBootTest
@AutoConfigureMockMvc
class MenuFlowIntegrationTest {

    private static final HikariDataSource DATA_SOURCE = TestDatabase.create(true);

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATA_SOURCE::getJdbcUrl);
        registry.add("spring.datasource.username", DATA_SOURCE::getUsername);
        registry.add("spring.datasource.password", DATA_SOURCE::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousVisitorSeesMenuByCategoryWithoutInternalFields() throws Exception {
        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(3))
                .andExpect(jsonPath("$.categories[*].name").value(hasItem("Gà rán")))
                .andExpect(jsonPath("$.categories[0].items[0].name").value("Burger bò"))
                .andExpect(jsonPath("$.categories[0].items[0].price").value(45000))
                // BR-04: món tạm hết vẫn hiện nhưng đánh dấu không còn bán; BR-03: món ngừng bán không hiện.
                .andExpect(jsonPath("$.categories[0].items[?(@.name == 'Burger gà giòn')].available")
                        .value(false))
                .andExpect(content().string(not(containsString("Combo cũ"))))
                .andExpect(content().string(not(containsString("workload"))));
    }

    @Test
    void singleItemEndpointKnowsSoldOutAndHidesDiscontinued() throws Exception {
        String body =
                mockMvc.perform(get("/api/menu")).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$.categories[0].items[?(@.name == 'Burger gà giòn')].id");
        long soldOutId = ids.getFirst();

        mockMvc.perform(get("/api/menu/items/" + soldOutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
        mockMvc.perform(get("/api/menu/items/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MENU_ITEM_NOT_FOUND"));
    }
}
