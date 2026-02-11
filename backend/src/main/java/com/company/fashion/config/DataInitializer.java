package com.company.fashion.config;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingStatus;
import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.clothing.repository.ClothingRepository;
import com.company.fashion.modules.member.entity.Member;
import com.company.fashion.modules.member.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DataInitializer {

  @Bean
  CommandLineRunner seedData(MemberRepository memberRepository, ClothingRepository clothingRepository) {
    return args -> {
      if (memberRepository.count() == 0) {
        // 主播A - S码，X型身材，韩风休闲
        Member member = new Member();
        member.setName("DemoStreamerA");
        member.setBodyData("""
            {"version":2,"measurements":{"heightCm":168.0,"weightKg":49.0,"shoulderWidthCm":38.0,"bustCm":84.0,
            "waistCm":62.0,"hipCm":89.0,
            "bodyShape":"X","legRatio":"long","topSize":"S","bottomSize":"S"}}""");
        member.setStyleTags("casual,korean");
        member.setPhotoUrl("https://pics.youliaolive.cn/fashion/member/2026/02/10/model.png");
        memberRepository.save(member);

        // 主播B - M码，H型身材，街头极简风
        Member memberB = new Member();
        memberB.setName("DemoStreamerB");
        memberB.setBodyData("""
            {"version":2,"measurements":{"heightCm":173.0,"weightKg":56.0,"shoulderWidthCm":40.0,"bustCm":88.0,
            "waistCm":66.0,"hipCm":92.0,
            "bodyShape":"H","legRatio":"regular","topSize":"M","bottomSize":"M"}}""");
        memberB.setStyleTags("street,minimal");
        memberB.setPhotoUrl("https://pics.youliaolive.cn/fashion/member/2026/02/10/model.png");
        memberRepository.save(memberB);

        // 主播C - S/M码，A型身材，法式优雅风
        Member memberC = new Member();
        memberC.setName("DemoStreamerC");
        memberC.setBodyData("""
            {"version":2,"measurements":{"heightCm":165.0,"weightKg":52.0,"shoulderWidthCm":37.0,"bustCm":82.0,
            "waistCm":64.0,"hipCm":90.0,
            "bodyShape":"A","legRatio":"regular","topSize":"S","bottomSize":"M"}}""");
        memberC.setStyleTags("elegant,french");
        memberC.setPhotoUrl("https://pics.youliaolive.cn/fashion/member/2026/02/10/model.png");
        memberRepository.save(memberC);

        // 主播D - L码，O型身材，街头嘻哈风
        Member memberD = new Member();
        memberD.setName("DemoStreamerD");
        memberD.setBodyData("""
            {"version":2,"measurements":{"heightCm":170.0,"weightKg":65.0,"shoulderWidthCm":42.0,"bustCm":96.0,
            "waistCm":78.0,"hipCm":98.0,
            "bodyShape":"O","legRatio":"short","topSize":"L","bottomSize":"L"}}""");
        memberD.setStyleTags("street,hiphop");
        memberD.setPhotoUrl("https://pics.youliaolive.cn/fashion/member/2026/02/10/model.png");
        memberRepository.save(memberD);
      }

      if (clothingRepository.count() == 0) {
        // S码上衣 - 适合主播A、C
        Clothing c1 = new Clothing();
        c1.setName("White T-Shirt");
        c1.setStyleTags("casual,summer");
        c1.setClothingType(ClothingType.TOP);
        c1.setStatus(ClothingStatus.ON_SHELF);
        c1.setSizeData("""
            {"size":"S","topSize":"S","fitType":"regular","shoulderWidthCm":38,"bustCm":84,"waistCm":62,"lengthCm":58,"sleeveLengthCm":18}""");
        c1.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/776d0e02-f8a9-472d-aa9c-878552548a1b-ChatGPT_Image_2026_2_10__11_38_38.png");
        clothingRepository.save(c1);

        // S码下装 - 适合主播A、C(上衣S)
        Clothing c2 = new Clothing();
        c2.setName("Light Blue Jeans");
        c2.setStyleTags("casual,denim");
        c2.setClothingType(ClothingType.BOTTOM);
        c2.setStatus(ClothingStatus.ON_SHELF);
        c2.setSizeData("""
            {"size":"S","bottomSize":"S","fitType":"regular","waistCm":62,"hipCm":88,"lengthCm":98,"inseamCm":76}""");
        c2.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/3ce29903-ce93-4655-a8e9-486b13013695-ChatGPT_Image_2026_2_10__11_39_17.png");
        clothingRepository.save(c2);

        // M码上衣 - 适合主播B
        Clothing c3 = new Clothing();
        c3.setName("Black Blazer");
        c3.setStyleTags("formal,business");
        c3.setClothingType(ClothingType.TOP);
        c3.setStatus(ClothingStatus.ON_SHELF);
        c3.setSizeData("""
            {"size":"M","topSize":"M","fitType":"regular","shoulderWidthCm":40,"bustCm":88,"waistCm":66,"lengthCm":68,"sleeveLengthCm":58}""");
        c3.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/b5cd94ec-aea6-41e9-aafd-e0499950404f-ChatGPT_Image_2026_2_10__11_40_04.png");
        clothingRepository.save(c3);

        // S码上衣 - 适合主播A、C
        Clothing c4 = new Clothing();
        c4.setName("Green Knit Cardigan");
        c4.setStyleTags("korean,soft");
        c4.setClothingType(ClothingType.TOP);
        c4.setStatus(ClothingStatus.ON_SHELF);
        c4.setSizeData("""
            {"size":"S","topSize":"S","fitType":"loose","shoulderWidthCm":39,"bustCm":88,"waistCm":64,"lengthCm":62,"sleeveLengthCm":55}""");
        c4.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/8b256e5f-dc8c-46f8-ae19-6758a92728c2-ChatGPT_Image_2026_2_10__11_41_24.png");
        clothingRepository.save(c4);

        // M码下装 - 适合主播B、C(下装M)
        Clothing c5 = new Clothing();
        c5.setName("Cargo Pants");
        c5.setStyleTags("street,utility");
        c5.setClothingType(ClothingType.BOTTOM);
        c5.setStatus(ClothingStatus.ON_SHELF);
        c5.setSizeData("""
            {"size":"M","bottomSize":"M","fitType":"loose","waistCm":66,"hipCm":92,"lengthCm":102,"inseamCm":78}""");
        c5.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/e4eda285-aa18-4bcc-9f83-f2099ff15f64-ChatGPT_Image_2026_2_10__11_41_55.png");
        clothingRepository.save(c5);

        // M码上衣 - 适合主播B，街头风格
        Clothing c6 = new Clothing();
        c6.setName("Oversized Hoodie");
        c6.setStyleTags("street,casual");
        c6.setClothingType(ClothingType.TOP);
        c6.setStatus(ClothingStatus.ON_SHELF);
        c6.setSizeData("""
            {"size":"M","topSize":"M","fitType":"oversized","shoulderWidthCm":58,"bustCm":108,"waistCm":100,"lengthCm":72,"sleeveLengthCm":62}""");
        c6.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/f5a633c8-4c3f-4b06-972f-30b6acd31206-ChatGPT_Image_2026_2_10__15_43_27.png");
        clothingRepository.save(c6);

        // L码下装 - 适合主播D
        Clothing c7 = new Clothing();
        c7.setName("Wide Leg Pants");
        c7.setStyleTags("street,hiphop");
        c7.setClothingType(ClothingType.BOTTOM);
        c7.setStatus(ClothingStatus.ON_SHELF);
        c7.setSizeData("""
            {"size":"L","bottomSize":"L","fitType":"loose","waistCm":75,"hipCm":99,"lengthCm":105,"inseamCm":80}""");
        c7.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/835d6b07-39f6-42c3-ab84-135fb6ec2172-ChatGPT_Image_2026_2_10__15_44_13.png");
        clothingRepository.save(c7);

        // L码上衣 - 适合主播D
        Clothing c8 = new Clothing();
        c8.setName("Graphic Tee");
        c8.setStyleTags("street,hiphop");
        c8.setClothingType(ClothingType.TOP);
        c8.setStatus(ClothingStatus.ON_SHELF);
        c8.setSizeData("""
            {"size":"L","topSize":"L","fitType":"loose","shoulderWidthCm":43,"bustCm":97,"waistCm":79,"lengthCm":70,"sleeveLengthCm":22}""");
        c8.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/750680a5-2f62-4bc9-acc6-7317991596b5-ChatGPT_Image_2026_2_10__15_48_07.png");
        clothingRepository.save(c8);

        // M码下装 - 适合主播B、C
        Clothing c9 = new Clothing();
        c9.setName("Pleated Skirt");
        c9.setStyleTags("elegant,french");
        c9.setClothingType(ClothingType.BOTTOM);
        c9.setStatus(ClothingStatus.ON_SHELF);
        c9.setSizeData("""
            {"size":"M","bottomSize":"M","fitType":"regular","waistCm":66,"hipCm":92,"lengthCm":72}""");
        c9.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/f0761cb2-e122-41c9-94a6-b0ea302cc055-ChatGPT_Image_2026_2_10__15_48_49.png");
        clothingRepository.save(c9);

        // S码上衣 - 适合主播A、C，法式风格
        Clothing c10 = new Clothing();
        c10.setName("Silk Blouse");
        c10.setStyleTags("elegant,french");
        c10.setClothingType(ClothingType.TOP);
        c10.setStatus(ClothingStatus.ON_SHELF);
        c10.setSizeData("""
            {"size":"S","topSize":"S","fitType":"regular","shoulderWidthCm":37,"bustCm":84,"waistCm":62,"lengthCm":60,"sleeveLengthCm":56}""");
        c10.setImageUrl("https://pics.youliaolive.cn/fashion/clothing/2026/02/10/8386246c-6fff-4617-8306-f4abc751cade-ChatGPT_Image_2026_2_10__15_50_02.png");
        clothingRepository.save(c10);
      }
    };
  }
}
