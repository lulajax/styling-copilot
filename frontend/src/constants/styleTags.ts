export interface StyleTagOption {
  value: StyleTagValue;
  labelKey: string;
}

export const STYLE_TAG_VALUES = [
  'kpop_sweet_cool',
  'street_dance',
  'sexy_stage',
  'school_youth',
  'techwear_future',
  'minimal_chic',
  'y2k_retro',
  'glam_stage',
  'fresh_summer',
  'winter_soft'
] as const;

export type StyleTagValue = (typeof STYLE_TAG_VALUES)[number];

export const STYLE_TAG_OPTIONS: StyleTagOption[] = [
  { value: 'kpop_sweet_cool', labelKey: 'styleTags.options.kpop_sweet_cool' },
  { value: 'street_dance', labelKey: 'styleTags.options.street_dance' },
  { value: 'sexy_stage', labelKey: 'styleTags.options.sexy_stage' },
  { value: 'school_youth', labelKey: 'styleTags.options.school_youth' },
  { value: 'techwear_future', labelKey: 'styleTags.options.techwear_future' },
  { value: 'minimal_chic', labelKey: 'styleTags.options.minimal_chic' },
  { value: 'y2k_retro', labelKey: 'styleTags.options.y2k_retro' },
  { value: 'glam_stage', labelKey: 'styleTags.options.glam_stage' },
  { value: 'fresh_summer', labelKey: 'styleTags.options.fresh_summer' },
  { value: 'winter_soft', labelKey: 'styleTags.options.winter_soft' }
];
