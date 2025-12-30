import RootComponent from './components/RootComponent.vue';
import { helperFunction } from '@/plugins/helper';

const instance = createInstance(RootComponent);
helperFunction(instance);

export { instance };
