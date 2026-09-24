-- 将防治手段扩展为农业、物理、化学、生物四类，与 farm 详情页展示结构保持一致。
ALTER TABLE `disease_control`
    DROP CHECK `chk_disease_control_category`,
    ADD CONSTRAINT `chk_disease_control_category`
        CHECK (`control_category` IS NULL OR `control_category` IN (1, 2, 3, 4));

-- 根据现有 name 补齐病虫害知识字段。详情图库暂复用已入库封面，避免引入来源不明的图片。
UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '春秋低温高湿期，叶面持续结露时易流行',
    `living_habits` = '病原可随病残体或种子存留，条件适宜时产生孢子并反复侵染叶片。',
    `suitable_environment` = '温度约15—22℃、空气湿度高、叶面结露且通风不良的环境。',
    `transmission_route` = '孢子可随气流、飞溅水滴、带菌种苗和作业工具传播。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 10,
    `remark` = '以降低湿度、减少叶面结露和及时清除病叶为防控重点。'
WHERE `id` = 3 AND `name` = '霜霉病';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '夏秋高温高湿期及移栽、采收造成伤口后易发生',
    `living_habits` = '软腐细菌可在病残体、栽培设施和水体中存活，并从伤口或自然孔口侵入。',
    `suitable_environment` = '温度约25—32℃、湿度高、组织有伤口或积水缺氧的环境。',
    `transmission_route` = '随灌溉水、雨水飞溅、带菌种苗、昆虫和污染工具传播。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 20,
    `remark` = '软腐病为细菌性病害，发现病株后应立即隔离并清洁消毒。'
WHERE `id` = 4 AND `name` = '软腐病';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '苗期至莲座期，夏季营养液温度偏高或根区长期缺氧时高发',
    `living_habits` = '腐霉、疫霉或镰刀菌等可在根残体、基质和循环水中存活并侵染新根。',
    `suitable_environment` = '根区温度偏高、溶氧不足、营养液循环不畅或基质长期过湿。',
    `transmission_route` = '随污染水源、循环营养液、带菌苗和未消毒器具扩散。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 30,
    `remark` = '应先排查根区温度、溶氧和水质，再结合病原诊断采取措施。'
WHERE `id` = 5 AND `name` = '根腐病';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '冬春低温高湿期，植株郁闭和结露持续时易发生',
    `living_habits` = '菌核可在病残体或设施缝隙中长期存活，萌发后形成菌丝或子囊孢子侵染。',
    `suitable_environment` = '温度约15—20℃、湿度高、通风差、植株密度过大的环境。',
    `transmission_route` = '子囊孢子随气流传播，菌核和菌丝可随病残体、基质和工具扩散。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 40,
    `remark` = '白色菌丝和黑色菌核是田间识别的重要特征。'
WHERE `id` = 6 AND `name` = '菌核病';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '春秋温暖期及设施内全年均可发生',
    `living_habits` = '多在嫩叶背面和生长点群集刺吸，繁殖快，常出现世代重叠。',
    `suitable_environment` = '温度约16—25℃、植株嫩绿、氮肥偏多且天敌较少的环境。',
    `transmission_route` = '有翅蚜主动迁飞，也可随带虫种苗、人员和工具进入设施。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 50,
    `remark` = '蚜虫可传播多种植物病毒，应在低虫口阶段及时控制。'
WHERE `id` = 7 AND `name` = '蚜虫';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '春末至秋季，设施栽培条件下可连续发生',
    `living_habits` = '成虫在叶组织内产卵，幼虫潜入叶肉取食并形成弯曲虫道，老熟后化蛹。',
    `suitable_environment` = '温度约20—30℃、寄主叶片鲜嫩且设施连续种植的环境。',
    `transmission_route` = '成虫迁飞扩散，也可随带虫叶片、种苗和运输物料传播。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 60,
    `remark` = '应在虫道较短、幼虫低龄时防治，并保护潜蝇茧蜂等天敌。'
WHERE `id` = 8 AND `name` = '斑潜蝇';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '夏秋温暖期，设施内可周年发生并世代重叠',
    `living_habits` = '成虫和若虫多聚集于叶背刺吸汁液，分泌蜜露并诱发煤污。',
    `suitable_environment` = '温度约22—30℃、通风不良、植株茂密且连续栽培的环境。',
    `transmission_route` = '成虫迁飞，亦可随带虫种苗、叶片和人员物资进入设施。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 70,
    `remark` = '白粉虱和烟粉虱形态接近，管理上均应兼顾成虫与若虫。'
WHERE `id` = 9 AND `name` = '白粉虱/烟粉虱';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '春末至秋季，温暖偏干时易快速增殖',
    `living_habits` = '成虫和若虫隐蔽于嫩叶、叶缝刺吸取食，部分种类在土表或残体中化蛹。',
    `suitable_environment` = '温度约23—28℃、空气偏干、杂草较多且组织幼嫩的环境。',
    `transmission_route` = '成虫主动迁飞，也可随种苗、花材、杂草和作业人员传播。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 80,
    `remark` = '蓟马隐蔽性强，应重点检查心叶和叶片褶皱处。'
WHERE `id` = 10 AND `name` = '蓟马';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '春秋两季为主，气温适中时幼虫危害明显',
    `living_habits` = '成虫在寄主附近活动产卵，幼虫取食叶片，小菜蛾受惊时常吐丝下垂。',
    `suitable_environment` = '温度约18—28℃、十字花科作物连作且防虫设施不完善的环境。',
    `transmission_route` = '成虫迁飞进入田间或设施，也可随带卵、带虫菜苗和叶片传播。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 90,
    `remark` = '在低龄幼虫期防治效果较好，并应轮换不同作用机制的药剂。'
WHERE `id` = 11 AND `name` = '小菜蛾/菜青虫';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `occurrence_period` = '春季和秋季，温暖干燥时成虫活动频繁',
    `living_habits` = '成虫善跳跃，取食叶片形成小孔，幼虫多在土壤或根际取食细根。',
    `suitable_environment` = '温暖干燥、十字花科连作、田边杂草多且表土疏松的环境。',
    `transmission_route` = '成虫跳跃或飞行扩散，也可随带虫种苗、土壤和栽培基质传播。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 100,
    `remark` = '苗期耐受能力弱，应重点保护幼苗和新展开叶片。'
WHERE `id` = 12 AND `name` = '跳甲';

UPDATE `disease_pest`
SET `affected_crops` = '西兰花、花椰菜等十字花科花菜类',
    `type` = 3,
    `cause` = '根系吸收或植株体内钙运输不能满足快速生长的心叶需求，常与高温、根区盐分偏高、供水波动、湿度过高导致蒸腾不足等因素共同相关。',
    `occurrence_period` = '花球形成和快速生长期，环境波动或生长过快时易发生',
    `living_habits` = '非传染性生理障碍，不存在病原生活史，也不会在植株间繁殖。',
    `suitable_environment` = '高温或湿度过高、根区盐分偏高、供水不均和通风不足的环境。',
    `transmission_route` = '不传播，通常由同一批次植株共同遭遇根区或环境胁迫而成片出现。',
    `image_urls` = JSON_ARRAY(`cover_image`),
    `sort_order` = 110,
    `remark` = '应通过稳定水肥和促进钙运输纠正，不能按传染性病害处理。'
WHERE `id` = 13 AND `name` = '烧边';

-- 兼容历史软腐病措施，使其纳入农业防治分组，后续再由种子表统一补齐内容。
UPDATE `disease_control`
SET `control_category` = 1
WHERE `disease_id` = 4 AND `control_category` IS NULL;

-- 临时种子表承载知识数据。用 disease_id + control_category 匹配，避免重复执行产生重复措施。
CREATE TEMPORARY TABLE `disease_control_seed` (
    `disease_id` bigint NOT NULL,
    `control_type` tinyint NOT NULL,
    `control_category` tinyint NOT NULL,
    `method` text NOT NULL,
    `drug_name` varchar(100) DEFAULT NULL,
    `usage_method` text,
    `dosage_spec` varchar(255) DEFAULT NULL,
    `safety_interval_days` smallint unsigned DEFAULT NULL,
    `precautions` varchar(500) DEFAULT NULL,
    `suitable_stage` varchar(100) DEFAULT NULL,
    `status` tinyint NOT NULL DEFAULT 1,
    `sort_order` int NOT NULL DEFAULT 0,
    `remark` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`disease_id`, `control_category`)
);

INSERT INTO `disease_control_seed`
(`disease_id`, `control_type`, `control_category`, `method`, `drug_name`, `usage_method`, `dosage_spec`, `safety_interval_days`, `precautions`, `suitable_stage`, `status`, `sort_order`, `remark`)
VALUES
(3,1,1,'选用健康种苗，合理密植，优先滴灌或根部供液，及时清除病叶并降低夜间湿度。',NULL,'定植后持续监测叶背，通风后再进行灌溉。',NULL,NULL,'病残体装袋移出，不在设施内堆放。','苗期至花球形成期',1,10,'预防为主'),
(3,1,2,'通风口设置防虫网并加强空气循环，清晨及时排湿，避免叶面长时间结露。',NULL,'根据温湿度自动或人工启闭通风设备。',NULL,NULL,'避免强风直吹造成植株机械损伤。','全生育期',1,20,'环境调控'),
(3,2,3,'发病初期选用目标作物和霜霉病上已登记的保护性或内吸性杀菌剂。','烯酰吗啉、氟吡菌胺·霜霉威','均匀喷施叶片正反面，重点覆盖叶背。','按产品登记标签用量',NULL,'严格按标签适用作物、次数和安全间隔期使用，轮换作用机制。','发病初期',1,30,'化学防治'),
(3,2,4,'发病前或初期可选用已登记的微生物制剂辅助抑制病原，配合降湿措施。','枯草芽孢杆菌类制剂','按登记标签进行预防性喷施。','按产品登记标签用量',NULL,'微生物制剂避免与不相容杀菌剂同时使用。','发病前至初期',1,40,'生物防治'),
(4,1,1,'使用无病种苗，控制营养液温度和根区积水，操作时减少伤口，病株立即移出。',NULL,'每茬结束后清洗栽培槽、管道和工具。',NULL,NULL,'病株及污染残体单独处理，避免汁液接触健康植株。','幼苗期至成熟期',1,10,'预防为主'),
(4,1,2,'改善通风排湿，使用专用工具清除病组织，对污染器具和接触面及时清洁消毒。',NULL,'先移除病株，再由清洁区向污染区作业。',NULL,NULL,'消毒剂不得直接混入营养液或接触可食用部位。','发病初期',1,20,'卫生防控'),
(4,2,3,'确认细菌性软腐后，选用目标作物和软腐病上已登记的细菌性病害防治药剂。','春雷·王铜、噻唑锌','发病初期喷施植株易感部位，并同步处理伤口和污染源。','按产品登记标签用量',NULL,'不得超范围、超剂量使用，严格执行标签安全间隔期。','发病初期',1,30,'化学防治'),
(4,2,4,'可选用已登记的芽孢杆菌类微生物制剂进行预防或辅助防治。','枯草芽孢杆菌类制剂','在低发病压力下按标签施用，并配合卫生清洁。','按产品登记标签用量',NULL,'生物制剂不能替代病株清除和环境消毒。','苗期至发病初期',1,40,'生物防治'),
(5,1,1,'使用健康种苗和洁净水源，保持根区供氧与循环通畅，定期清洁营养液系统。',NULL,'监测根区温度、溶氧、电导率和酸碱度并及时纠偏。',NULL,NULL,'先纠正根区缺氧和高温，避免只依赖药剂。','苗期至莲座期',1,10,'根区管理'),
(5,1,2,'移除严重病株和腐根，清洁栽培槽与管路，必要时分区隔离循环水。',NULL,'停止污染区回液进入健康区，清除堵塞物。',NULL,NULL,'处理病根后及时清洁工具，防止交叉污染。','发病初期',1,20,'物理隔离'),
(5,2,3,'经诊断属于腐霉或疫霉类根腐后，选用相应作物和病害上已登记的杀菌剂。','精甲霜灵类或恶霉灵类登记制剂','按登记标签灌根或处理根区。','按产品登记标签用量',NULL,'病原未明确前不盲目混药，严格执行标签和水培系统适用要求。','发病初期',1,30,'对症用药'),
(5,1,4,'定植前可使用已登记的有益微生物制剂改善根际微生态，抑制土传或水传病原。','木霉菌、枯草芽孢杆菌类制剂','按标签进行根区处理或基质处理。','按产品登记标签用量',NULL,'活菌制剂避免与强氧化消毒剂或不相容杀菌剂同时使用。','定植前至苗期',1,40,'生物防治'),
(6,1,1,'合理密植并及时清除病株和落叶，收获后彻底清除菌核与病残体。',NULL,'先清理发病中心，再清洁周边栽培面。',NULL,NULL,'病残体不得就地堆沤，防止菌核遗留。','全生育期',1,10,'清洁田园'),
(6,1,2,'加强通风、降低结露，发现局部病株后及时隔离并清洁接触面。',NULL,'控制夜间湿度并避免叶片相互贴靠。',NULL,NULL,'移除病株时避免抖落菌核。','莲座期至成熟期',1,20,'环境调控'),
(6,2,3,'发病初期选用目标作物和菌核病上已登记的杀菌剂，重点处理茎基部和近地叶片。','啶酰菌胺、异菌脲类登记制剂','均匀喷施易感部位，按标签限制次数。','按产品登记标签用量',NULL,'轮换不同作用机制，严格执行安全间隔期。','发病初期',1,30,'化学防治'),
(6,1,4,'休茬或定植前可选用已登记的盾壳霉、木霉等微生物制剂降低菌核活力。','盾壳霉、木霉菌类制剂','按标签处理基质或发病区域。','按产品登记标签用量',NULL,'保持适宜活菌条件，避免与不相容杀菌剂混用。','休茬期至定植前',1,40,'生物防治'),
(7,1,1,'培育健壮苗，控制氮肥偏施，及时清除虫叶和寄主杂草，保护瓢虫等天敌。',NULL,'每周检查嫩叶背面和生长点。',NULL,NULL,'发现有翅蚜后增加监测频次。','苗期至成熟期',1,10,'农业防治'),
(7,1,2,'通风口安装防虫网，悬挂黄色粘虫板监测并诱杀有翅蚜。',NULL,'黄板略高于植株顶部并随株高调整。',NULL,NULL,'粘虫板达到一定虫量后及时更换。','全生育期',1,20,'物理防治'),
(7,2,3,'虫口上升时选用目标作物和蚜虫上已登记的选择性杀虫剂，重点喷施叶背。','氟啶虫酰胺、吡蚜酮类登记制剂','低虫口期均匀喷雾并轮换作用机制。','按产品登记标签用量',NULL,'保护授粉昆虫和天敌，严格执行标签安全间隔期。','发生初期',1,30,'化学防治'),
(7,1,4,'释放蚜茧蜂、瓢虫等天敌，或使用已登记的苦参碱等生物源制剂。','蚜茧蜂、苦参碱类制剂','按虫量连续释放天敌或按标签喷施。','按天敌产品或农药标签用量',NULL,'释放天敌前后避免使用广谱杀虫剂。','低虫口期',1,40,'生物防治'),
(8,1,1,'及时摘除虫道密集叶片并清除残株，减少连续种植和虫源积累。',NULL,'幼虫尚未老熟前移除受害叶片。',NULL,NULL,'摘叶量不宜影响植株正常生长。','苗期至莲座期',1,10,'农业防治'),
(8,1,2,'安装防虫网并悬挂黄色粘虫板监测、诱杀斑潜蝇成虫。',NULL,'黄板随植株高度调整并定期统计虫量。',NULL,NULL,'保持出入口缓冲区关闭。','全生育期',1,20,'物理防治'),
(8,2,3,'在低龄幼虫形成短虫道时，选用目标作物和斑潜蝇上已登记的药剂。','灭蝇胺、多杀霉素类登记制剂','重点覆盖新叶和虫道初发叶片。','按产品登记标签用量',NULL,'轮换作用机制，避免在同一世代连续重复用药。','幼虫低龄期',1,30,'化学防治'),
(8,1,4,'保护或释放潜蝇茧蜂等寄生性天敌，优先在低虫口阶段建立种群。','潜蝇茧蜂等天敌','按虫口密度分批释放。','按天敌产品技术要求',NULL,'释放前后避免使用对寄生蜂高风险的广谱药剂。','发生初期',1,40,'生物防治'),
(9,1,1,'使用无虫苗，及时清除老叶、虫叶和设施周边杂草，避免不同茬口虫源衔接。',NULL,'定植前检查叶背，结束后彻底清园。',NULL,NULL,'带虫残体密闭移出设施。','全生育期',1,10,'农业防治'),
(9,1,2,'通风口安装60—80目防虫网，悬挂黄色粘虫板监测并诱杀成虫。',NULL,'黄板设置在植株上方并定期更换。',NULL,NULL,'防虫网过密时应兼顾通风降温。','全生育期',1,20,'物理防治'),
(9,2,3,'若虫发生初期选用目标作物和粉虱上已登记的选择性药剂，重点覆盖叶背。','螺虫乙酯、呋虫胺类登记制剂','按标签喷雾，兼顾不同虫态并轮换作用机制。','按产品登记标签用量',NULL,'避免连续使用同一作用机制，严格执行安全间隔期。','若虫发生初期',1,30,'化学防治'),
(9,1,4,'释放丽蚜小蜂、烟盲蝽等天敌，或选用已登记的球孢白僵菌制剂。','丽蚜小蜂、球孢白僵菌类制剂','低虫口期分批释放或按标签喷施。','按天敌产品或农药标签用量',NULL,'生物防治期间避免使用高残效广谱杀虫剂。','发生初期',1,40,'生物防治'),
(10,1,1,'清除设施内外杂草和残株，使用无虫苗，避免氮肥过量造成组织过嫩。',NULL,'重点检查心叶、叶缝和新展开叶。',NULL,NULL,'发现虫株后及时标记并缩短检查间隔。','全生育期',1,10,'农业防治'),
(10,1,2,'安装防虫网，悬挂蓝色粘虫板进行监测和诱杀。',NULL,'蓝板略高于植株顶部并均匀布设。',NULL,NULL,'粘板仅作监测和压低虫口，不能替代其他措施。','全生育期',1,20,'物理防治'),
(10,2,3,'发生初期选用目标作物和蓟马上已登记的药剂，细致喷施心叶和叶片褶皱。','乙基多杀菌素、多杀霉素类登记制剂','上午较早时段或傍晚均匀喷雾。','按产品登记标签用量',NULL,'轮换作用机制，严格遵守标签施药次数和安全间隔期。','发生初期',1,30,'化学防治'),
(10,1,4,'释放小花蝽、捕食螨等天敌，或在发生初期使用白僵菌、绿僵菌类微生物制剂。','小花蝽、捕食螨、白僵菌类制剂','根据虫口分批释放，微生物制剂宜按标签在适宜时段施用。','按天敌产品或农药标签用量',NULL,'微生物杀虫剂不与化学杀菌剂混用。','低虫口期',1,40,'生物防治'),
(11,1,1,'合理安排茬口，及时清除卵块、虫叶和残株，结合巡查人工捕捉幼虫。',NULL,'每周检查叶背和心叶，重点寻找低龄幼虫。',NULL,NULL,'虫叶密闭移出，避免幼虫转移。','苗期至成熟期',1,10,'农业防治'),
(11,1,2,'通风口安装防虫网，利用性诱捕器监测小菜蛾成虫并辅助压低虫量。',NULL,'按产品要求布设诱捕器并定期更换诱芯。',NULL,NULL,'性诱仅针对相应害虫，需结合田间调查。','全生育期',1,20,'物理防治'),
(11,2,3,'低龄幼虫期选用目标作物和小菜蛾或菜青虫上已登记的药剂。','氯虫苯甲酰胺、甲氨基阿维菌素苯甲酸盐类登记制剂','均匀喷施叶背和心叶，轮换作用机制。','按产品登记标签用量',NULL,'避免在高龄幼虫期盲目加量，严格执行安全间隔期。','低龄幼虫期',1,30,'化学防治'),
(11,1,4,'低龄幼虫期优先使用苏云金杆菌制剂，并保护赤眼蜂等寄生性天敌。','苏云金杆菌类制剂','按标签均匀喷施幼虫取食部位。','按产品登记标签用量',NULL,'苏云金杆菌对低龄幼虫效果较好，施用后避免强烈日晒冲刷。','低龄幼虫期',1,40,'生物防治'),
(12,1,1,'清除十字花科杂草和残株，避免连作，定植前检查种苗与根际基质。',NULL,'苗期高频巡查新叶小孔和成虫活动。',NULL,NULL,'受害严重幼苗及时补栽并清理虫源。','苗期',1,10,'农业防治'),
(12,1,2,'使用防虫网阻隔成虫，苗床可采用合规覆盖材料保护幼苗。',NULL,'保持覆盖完整，操作后及时闭合。',NULL,NULL,'兼顾通风和温度，避免覆盖造成高温伤苗。','苗期',1,20,'物理防治'),
(12,2,3,'成虫盛发或幼虫危害根系时，选用目标作物和跳甲上已登记的药剂对症处理。','呋虫胺、啶虫脒类登记制剂','按标签喷雾或采用获准的根区处理方式。','按产品登记标签用量',NULL,'严禁自行改变施药方式，严格执行安全间隔期。','苗期至发生初期',1,30,'化学防治'),
(12,1,4,'可选用已登记的白僵菌制剂，或使用昆虫病原线虫辅助控制土中幼虫。','白僵菌类制剂、昆虫病原线虫','在适宜温湿度下按产品技术要求处理。','按产品登记或技术要求',NULL,'避免与不相容杀菌剂同时使用，保持生物制剂活性。','发生初期',1,40,'生物防治'),
(13,1,1,'保持供水和营养液浓度稳定，避免氮肥过量与根区盐分过高，分次补充钙源。',NULL,'连续监测电导率、酸碱度和根区温度，避免水肥大幅波动。',NULL,NULL,'先确认根系健康，钙供应充足不代表心叶一定能获得足量钙。','快速生长期至花球形成期',1,10,'水肥调控'),
(13,1,2,'改善冠层通风和空气流动，在高湿时促进适度蒸腾，高温时避免叶温过高。',NULL,'调整风机和遮阳，保持环境变化平缓。',NULL,NULL,'避免强风直吹和突然降低湿度造成新的胁迫。','快速生长期至花球形成期',1,20,'环境调控'),
(13,2,3,'缺钙风险明确时可按肥料标签补充适宜钙源，但不能把叶面喷钙作为唯一措施。','合规钙肥','结合水质和配方调整根施或叶面补充方式。','按肥料产品标签用量',NULL,'不得与易产生沉淀的浓缩母液直接混合，先做小范围兼容性试验。','症状发生前至初期',1,30,'营养调控');

-- 更新同病虫害、同防治手段的历史记录，保留原主键和创建时间。
UPDATE `disease_control` dc
JOIN `disease_control_seed` seed
  ON seed.`disease_id` = dc.`disease_id`
 AND seed.`control_category` = dc.`control_category`
SET dc.`control_type` = seed.`control_type`,
    dc.`method` = seed.`method`,
    dc.`drug_name` = seed.`drug_name`,
    dc.`usage_method` = seed.`usage_method`,
    dc.`dosage_spec` = seed.`dosage_spec`,
    dc.`safety_interval_days` = seed.`safety_interval_days`,
    dc.`precautions` = seed.`precautions`,
    dc.`suitable_stage` = seed.`suitable_stage`,
    dc.`status` = seed.`status`,
    dc.`sort_order` = seed.`sort_order`,
    dc.`remark` = seed.`remark`;

-- 仅插入尚不存在的分类措施，确保每条 disease_id 都指向真实的 disease_pest 主键。
INSERT INTO `disease_control`
(`disease_id`, `control_type`, `control_category`, `method`, `drug_name`, `usage_method`, `dosage_spec`, `safety_interval_days`, `precautions`, `suitable_stage`, `status`, `sort_order`, `remark`)
SELECT seed.`disease_id`, seed.`control_type`, seed.`control_category`, seed.`method`, seed.`drug_name`,
       seed.`usage_method`, seed.`dosage_spec`, seed.`safety_interval_days`, seed.`precautions`,
       seed.`suitable_stage`, seed.`status`, seed.`sort_order`, seed.`remark`
FROM `disease_control_seed` seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `disease_control` dc
    WHERE dc.`disease_id` = seed.`disease_id`
      AND dc.`control_category` = seed.`control_category`
);

DROP TEMPORARY TABLE `disease_control_seed`;
