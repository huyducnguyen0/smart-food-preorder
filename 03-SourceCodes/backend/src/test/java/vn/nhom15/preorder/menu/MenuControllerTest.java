package vn.nhom15.preorder.menu;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nhom15.preorder.auth.AuthService;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.config.SecurityConfig;

/** Lớp web của UC-03: công khai, JSON đúng hợp đồng API, không lộ điểm công việc. */
@WebMvcTest(MenuController.class)
@Import(SecurityConfig.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Bộ lọc tài khoản bị khóa (BR-32) hỏi AuthService ở mỗi yêu cầu; mock trả false: không bị khóa. */
    @MockitoBean
    AuthService authService;

    @MockitoBean
    private MenuService menuService;

    private static MenuItem item(long id, String name, boolean available) {
        return new MenuItem(id, 1, name, null, null, 45000, 5, available, false);
    }

    @Test
    void menuIsPublicAndMatchesTheApiContract() throws Exception {
        when(menuService.getMenu())
                .thenReturn(List.of(new MenuCategory(
                        1, "Burger", List.of(item(12, "Burger bò", true), item(13, "Burger gà giòn", false)))));

        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].name").value("Burger"))
                .andExpect(jsonPath("$.categories[0].items[0].id").value(12))
                .andExpect(jsonPath("$.categories[0].items[0].price").value(45000))
                .andExpect(jsonPath("$.categories[0].items[0].available").value(true))
                .andExpect(jsonPath("$.categories[0].items[1].available").value(false));
    }

    @Test
    void menuNeverExposesWorkloadPoints() throws Exception {
        when(menuService.getMenu())
                .thenReturn(List.of(new MenuCategory(1, "Burger", List.of(item(12, "Burger bò", true)))));

        mockMvc.perform(get("/api/menu"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("workload"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("discontinued"))));
    }

    @Test
    void emptyMenuIsNotAnError() throws Exception {
        when(menuService.getMenu()).thenReturn(List.of());

        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isEmpty());
    }

    @Test
    void singleItemReturnsCurrentPriceAndAvailability() throws Exception {
        when(menuService.getVisibleItem(13)).thenReturn(item(13, "Burger gà giòn", false));

        mockMvc.perform(get("/api/menu/items/13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Burger gà giòn"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void unknownItemReturns404WithCode() throws Exception {
        when(menuService.getVisibleItem(99))
                .thenThrow(new ApiException(ErrorCode.MENU_ITEM_NOT_FOUND, "Món không có trong thực đơn"));

        mockMvc.perform(get("/api/menu/items/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MENU_ITEM_NOT_FOUND"));
    }

    @Test
    void nonNumericItemIdIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/menu/items/abc")).andExpect(status().isBadRequest());
    }
}
