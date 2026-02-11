import { createI18n } from 'vue-i18n';
import en from './locales/en';
import zh from './locales/zh';
import ko from './locales/ko';

const messages = {
  en,
  zh,
  ko
};

const savedLocale = localStorage.getItem('locale');
const defaultLocale = savedLocale || 'en';

export const i18n = createI18n({
  legacy: false,
  locale: defaultLocale,
  fallbackLocale: 'en',
  messages
});

export type Locale = 'en' | 'zh' | 'ko';

export function setLocale(locale: Locale) {
  i18n.global.locale.value = locale;
  localStorage.setItem('locale', locale);
}

export function getLocale(): Locale {
  return i18n.global.locale.value as Locale;
}
