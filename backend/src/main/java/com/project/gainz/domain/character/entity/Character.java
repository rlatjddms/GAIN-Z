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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    // 리스닝 룸이 늘어 값 목록 제약 없는 varchar로 둔다 (CharacterSchemaPatcher가 예전 칸을 넓힘)
    @Convert(converter = StageBackgroundConverter.class)
    @Column(length = 20)
    private StageBackground stageBackground;

    // 무대에 올린 방 꾸미기 소품과 자리 [{ key, x }] (레코드 수집 보상)
    @Column(name = "decor_layout", columnDefinition = "TEXT")
    @Convert(converter = DecorLayoutConverter.class)
    private List<DecorPlacement> decorLayout = new ArrayList<>();

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

    @Column(length = 30)
    @Convert(converter = AccessoryConverter.class)
    private Accessory waist;

    // 그림에서 악기를 내려놓았는지 (악기 종류·밴드 포지션은 그대로). null = 예전 캐릭터 = 들고 있음
    @Column(name = "instrument_hidden")
    private Boolean instrumentHidden;

    // 아이템마다 고른 색 { "HOODIE": "#ffd8a8" } (없는 아이템은 기본 색)
    @Column(columnDefinition = "TEXT")
    @Convert(converter = ItemColorsConverter.class)
    private Map<String, String> itemColors = new TreeMap<>();

    // 고른 무대 효과 (레벨 보상, 없으면 null)
    @Column(length = 30)
    @Convert(converter = StageEffectConverter.class)
    private StageEffect stageEffect;     // 예전(효과 하나만 고르던 때) 값. 지금은 stageEffects

    // 켜 둔 무대 효과 (여러 개)
    @Column(name = "stage_effects", length = 100)
    @Convert(converter = StageEffectsConverter.class)
    private List<StageEffect> stageEffects = new ArrayList<>();

    // 입고 있는 하의 (null이면 기본 청바지. 예전 캐릭터)
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

    // 레벨 규칙이 바뀌었을 때 새 규칙의 레벨·경험치로 옮긴다 (CharacterLevelMigration)
    public void migrateLevel(int level, int exp, int ruleVersion) {
        this.level = level;
        this.exp = exp;
        this.levelRule = ruleVersion;
    }

    // 옷장: 얻은 무대 배경 중 하나 고르기 (해금 여부는 서비스에서 확인)
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
            case WAIST -> this.waist = accessory;
            case SHOES -> this.shoes = accessory;
            case BOTTOM -> this.bottom = accessory;
        }
    }

    // 입고 있는 하의·신발 (고른 적 없으면 기본 청바지·신발)
    public Accessory getWornBottom() {
        return bottom != null ? bottom : Accessory.defaultFor(AccessorySlot.BOTTOM);
    }

    public Accessory getWornShoes() {
        return shoes != null ? shoes : Accessory.defaultFor(AccessorySlot.SHOES);
    }

    public boolean isInstrumentHidden() {
        return Boolean.TRUE.equals(instrumentHidden);
    }

    public List<DecorPlacement> getDecorLayout() {
        return decorLayout != null ? List.copyOf(decorLayout) : List.of();
    }

    // 옷장: 방 꾸미기 소품 배치 (모은 여부·자리 범위는 서비스에서 확인)
    public void changeDecorLayout(List<DecorPlacement> layout) {
        this.decorLayout = new ArrayList<>(layout);
    }

    // 옷장: 악기 들기·내려놓기
    public void changeInstrumentHidden(boolean hidden) {
        this.instrumentHidden = hidden;
    }

    public Map<String, String> getItemColors() {
        return itemColors != null ? Map.copyOf(itemColors) : Map.of();
    }

    // 옷장: 아이템 색 바꾸기 (키·색 검사는 서비스에서)
    public void changeItemColors(Map<String, String> colors) {
        this.itemColors = new TreeMap<>(colors);
    }

    // 옷장: 무대 효과 고르기 (null이면 끄기, 해금 여부는 서비스에서 확인)
    public void changeStageEffects(List<StageEffect> effects) {
        this.stageEffects = new ArrayList<>(effects);
        this.stageEffect = null;
    }

    // 켜 둔 무대 효과. 예전에 하나만 골라 둔 캐릭터는 그 효과
    public List<StageEffect> getStageEffects() {
        if (stageEffects != null && !stageEffects.isEmpty()) return List.copyOf(stageEffects);
        return stageEffect != null ? List.of(stageEffect) : List.of();
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
