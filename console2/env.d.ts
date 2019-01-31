export {};

export interface ConcordEnvironment {
    topBar?: TopBarMeta;
}

export interface TopBarMeta {
    systemLinks?: SystemLinks;
}

export type SystemLinks = LinkMeta[];

export interface LinkMeta {
    text: string;
    url: string;
    icon?: string;
}

declare global {
    interface Window {
        concord: ConcordEnvironment;
    }
}
