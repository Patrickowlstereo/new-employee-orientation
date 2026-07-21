package com.gmnl.orientation.seed;

import com.gmnl.orientation.content.*;
import com.gmnl.orientation.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

  private final InstitutionRepository institutionRepo;
  private final IslandRepository islandRepo;
  private final DocRepository docRepo;
  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;
  private final String adminInitialPassword;

  public DataInitializer(InstitutionRepository institutionRepo,
                         IslandRepository islandRepo,
                         DocRepository docRepo,
                         UserRepository userRepo,
                         PasswordEncoder passwordEncoder,
                         @Value("${ADMIN_INITIAL_PASSWORD:}") String adminInitialPassword) {
    this.institutionRepo = institutionRepo;
    this.islandRepo = islandRepo;
    this.docRepo = docRepo;
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
    this.adminInitialPassword = adminInitialPassword;
  }

  /** 初始 admin 密码:优先取环境变量 ADMIN_INITIAL_PASSWORD;未配置时生成随机密码并仅打印一次。 */
  static String resolveAdminInitialPassword(String configured) {
    if (configured != null && !configured.isBlank()) return configured;
    return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  @Override
  public void run(String... args) {
    if (userRepo.count() > 0) return; // 已初始化则跳过

    // admin 账号:密码来自环境变量或随机生成,绝不硬编码
    String rawPassword = resolveAdminInitialPassword(adminInitialPassword);
    if (adminInitialPassword == null || adminInitialPassword.isBlank()) {
      log.warn("未配置 ADMIN_INITIAL_PASSWORD,已为初始 admin 账号生成随机密码(仅显示这一次): {}",
          rawPassword);
    }
    User admin = new User();
    admin.setUsername("admin");
    admin.setName("管理员");
    admin.setPasswordHash(passwordEncoder.encode(rawPassword));
    admin.setRole(UserRole.ADMIN);
    userRepo.save(admin);

    // 7 机构
    Map<String, String> instDefs = new java.util.LinkedHashMap<>();
    instDefs.put("BJ", "北京");
    instDefs.put("SH", "上海");
    instDefs.put("SD", "山东");
    instDefs.put("SC", "四川");
    instDefs.put("GD", "广东");
    instDefs.put("CQ", "重庆");
    instDefs.put("ZJ", "浙江");

    int order = 0;
    for (Map.Entry<String, String> e : instDefs.entrySet()) {
      Institution inst = new Institution();
      inst.setKey(e.getKey());
      inst.setName(e.getValue());
      inst.setOrder(order++);
      institutionRepo.save(inst);

      // 每个机构建同样的 5 小岛
      String[][] islandDefs = {
        {"about", "关于公司"},
        {"onboarding", "入职须知"},
        {"office", "办公指南"},
        {"products", "公司产品"},
        {"quiz", "知识测验"}
      };
      int io = 0;
      for (String[] d : islandDefs) {
        Island isl = new Island();
        isl.setKey(d[0] + "_" + e.getKey());
        isl.setName(d[1]);
        isl.setOrder(io++);
        isl.setInstitutionId(inst.getId());
        islandRepo.save(isl);

        // 每小岛 1 个占位文档（真实文件由后台后续上传）
        Doc doc = new Doc();
        doc.setTitle(d[1] + " - " + e.getValue() + "（示例）");
        doc.setCategory(d[1]);
        doc.setInstitutionId(inst.getId());
        doc.setIslandId(isl.getId());
        doc.setRequired(true);
        doc.setFilePath(null);
        doc.setFileType(null);
        doc.setOrder(0);
        doc.setActive(true);
        docRepo.save(doc);
      }
    }
  }
}
