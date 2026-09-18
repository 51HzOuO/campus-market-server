package com.example.campusmarketserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@SpringBootTest
class CampusMarketServerApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

    @Test
    void fixNoticeTableV2() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 修改 type 列为 VARCHAR(20)
            stmt.execute("ALTER TABLE notice MODIFY COLUMN type VARCHAR(20) NOT NULL COMMENT '通知类型: like/comment'");
            System.out.println("type 列已改为 VARCHAR(20)");

            // 检查 content 列
            ResultSet rs = conn.getMetaData().getColumns(null, null, "notice", "content");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE notice ADD COLUMN content VARCHAR(500) NOT NULL DEFAULT '' COMMENT '通知内容' AFTER type");
                System.out.println("已添加 content 列");
            } else {
                System.out.println("content 列已存在，类型: " + rs.getString("TYPE_NAME") + "(" + rs.getInt("COLUMN_SIZE") + ")");
            }
            rs.close();

            // 检查 is_read 列
            rs = conn.getMetaData().getColumns(null, null, "notice", "is_read");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE notice ADD COLUMN is_read INT DEFAULT 0 COMMENT '0未读 1已读' AFTER content");
                System.out.println("已添加 is_read 列");
            } else {
                System.out.println("is_read 列已存在");
            }
            rs.close();

            // 检查 create_time 列
            rs = conn.getMetaData().getColumns(null, null, "notice", "create_time");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE notice ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP AFTER is_read");
                System.out.println("已添加 create_time 列");
            } else {
                System.out.println("create_time 列已存在");
            }
            rs.close();

            // 打印最终表结构
            rs = conn.getMetaData().getColumns(null, null, "notice", null);
            System.out.println("\n=== notice 表最终结构 ===");
            while (rs.next()) {
                System.out.println(rs.getString("COLUMN_NAME") + " | " + rs.getString("TYPE_NAME") + "(" + rs.getInt("COLUMN_SIZE") + ") | " + rs.getString("IS_NULLABLE"));
            }
            rs.close();
            System.out.println("=========================");
        }
    }
}
