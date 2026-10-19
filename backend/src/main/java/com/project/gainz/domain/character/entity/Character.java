package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.Accessory;
import com.project.gainz.domain.character.enums.AccessorySlot;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.enums.LevelPolicy;
import com.project.gainz.domain.character.enums.StageBackground;
import com.project.gainz.domain.character.enums.StageEffect;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "characters")
@SQLDelete(sql = "UPDATE characters SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Character extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InstrumentType instrumentType;

    @Column(nullable = false, length = 7)
    private String instrumentColor;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    // 레벨 보상 머리가 늘어서 값 목록 제약 없는 varchar로 둔다 (EnumNameConverter, CharacterSchemaPatcher)
    @Column(nullable = false, length = 30)
    @Convert(converter = HairStyleConverter.class)
    private HairStyle hairStyle;

    @Column(nullable = false, length = 7)
    private String hairColor;

    @Column(nullable = false, length = 7, columnDefinition = "varchar(7) default '#F8D7B5'")
    private String skinColor;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Integer exp;

    // 이 캐릭터의 레벨이 어느 레벨 규칙 기준인지 (null = 예전 Lv.50 규칙, LevelPolicy.RULE_VERSION = 지금 규칙)
    @Column(name = "level_rule")
    private Integer levelRule;

    @Column(nullable = false)
    private Integer currentStreak;

    @Column(nullable = false)
    private Integer maxStreak;

    @Column(nullable = false)
    private Long totalPracticeTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_outfit_id", nullable = false)
    private OutfitItem currentOutfit;

    // 고른 무대 배경. 예전 캐릭터처럼 비어 있으면 레벨로 얻은 가장 큰 무대
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private StageBackground stageBackground;

    // 쓰고 있는 레벨 보상 소품 (자리마다 하나, 없으면 null)
    @Column(length = 30)
    @Convert(converter = AccessoryConverter.class)
    private Accessory hat;

    @Column(length = 30)
    @Convert(converter = AccessoryConverter.class)
    private Accessory glasses;

    @Column(length = 30)
    @Convert(converter = AccessoryConverter.class)
    private Accessory earrings;

    @Column(length = 30)
    @Convert(converter = AccessoryConverter.class)
    private Accessory neck;

    @Column(length = 30)
    @Convert(converter = AccessoryConverter.class)
    private Accessory shoes;

    // 고른 무대 효과 (레벨 보상, 없으면 null)
    @Column(length = 30)
    @Convert(converter = StageEffectConverter.class)
    private StageEffect stageEffect;

    // 입고 있는 레벨 보상 하의 (null이면 몸 그림의 기본 청바지)
    @Column(length = 30)
    @Convert(converter = AccessoryConverter.class)
    private Accessory bottom;

    @Version
    private Long version;

    public static final String DEFAULT_SKIN_COLOR = "#F8D7B5";

    @Builder
    public Character(User user, String nickname, InstrumentType instrumentType, String instrumentColor,
                     Gender gender, HairStyle hairStyle, String hairColor, String skinColor, OutfitItem currentOutfit) {
        this.user = user;
        this.nickname = nickname;
        this.instrumentType = instrumentType;
        this.instrumentColor = instrumentColor;
        this.gender = gender;
        this.hairStyle = hairStyle;
        this.hairColor = hairColor;
        this.skinColor = skinColor != null ? skinColor : DEFAULT_SKIN_COLOR;
        this.currentOutfit = currentOutfit;
        this.stageBackground = StageBackground.ROOKIE;
        this.level = 1;
        this.exp = 0;
        this.levelRule = LevelPolicy.RULE_VERSION;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.totalPracticeTime = 0L;
    }

    public void update(String nickname, InstrumentType instrumentType, String instrumentColor,
                       Gender gender, HairStyle hairStyle, String hairColor, String skinColor) {
        this.nickname = nickname;
        this.instrumentType = instrumentType;
        this.instrumentColor = instrumentColor;
        this.gender = gender;
        this.hairStyle = hairStyle;
        this.hairColor = hairColor;
        if (skinColor != null) this.skinColor = skinColor;
    }

    // 옷장: 의상 갈아입기 (보유 여부는 서비스에서 확인)
    public void changeOutfit(OutfitItem outfit) {
        this.currentOutfit = outfit;
    }

    // 옷장: 얻은 무대 배경 중 하나 고르기 (해금 여부는 서비스에서 확인)
    // 레벨 규칙이 바뀌었을 때 새 규칙의 레벨·경험치로 옮긴다 (CharacterLevelMigration)
    public void migrateLevel(int level, int exp, int ruleVersion) {
        this.level = level;
        this.exp = exp;
        this.levelRule = ruleVersion;
    }

    public void changeStageBackground(StageBackground background) {
        this.stageBackground = background;
    }

    // 옷장: 자리마다 소품·하의 쓰기·벗기 (자리·해금 여부는 서비스에서 확인)
    public void wear(AccessorySlot slot, Accessory accessory) {
        switch (slot) {
            case HAT -> this.hat = accessory;
            case GLASSES -> this.glasses = accessory;
            case EARRINGS -> this.earrings = accessory;
            case NECK -> this.neck = accessory;
            case SHOES -> this.shoes = accessory;
            case BOTTOM -> this.bottom = accessory;
        }
    }

    // 옷장: 무대 효과 고르기 (null이면 끄기, 해금 여부는 서비스에서 확인)
    public void changeStageEffect(StageEffect effect) {
        this.stageEffect = effect;
    }

    // 지금 무대 배경. 아직 고른 적 없으면 레벨로 얻은 가장 큰 무대
    public StageBackground getCurrentStageBackground() {
        return stageBackground != null ? stageBackground : StageBackground.highestUnlockedAt(level);
    }

    public boolean gainExperience(int amount) {

        if (this.level >= LevelPolicy.MAX_LEVEL) return false;

        this.exp += amount;
        boolean leveledUp = false;

        while (this.exp >= LevelPolicy.getRequiredExpForLevel(this.level)) {
            this.exp -= LevelPolicy.getRequiredExpForLevel(this.level);
            this.level++;
            leveledUp = true;

            if (this.level >= LevelPolicy.MAX_LEVEL) {
                this.exp = 0;
                break;
            }
        }
        return leveledUp;
    }

    // 연습 기록을 줄여 경험치를 되돌린다. 레벨은 내리지 않고, 모자라면 경험치가 음수(부족분)가 되어
    // 다음 레벨업까지 그만큼 더 채워야 한다. 최대 레벨이면 그대로 둔다
    public void loseExperience(int amount) {
        if (amount <= 0 || this.level >= LevelPolicy.MAX_LEVEL) return;
        this.exp -= amount;
    }

    // 연습 기록을 직접 줄이면 음수가 들어올 수 있다 (0 아래로는 내려가지 않음)
    public void addPracticeTime(long minutes) {
        this.totalPracticeTime = Math.max(0, this.totalPracticeTime + minutes);
    }
}
