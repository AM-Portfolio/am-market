package com.marketdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for market data services
 */
@Configuration
@ConfigurationProperties(prefix = "market-data")
public class MarketDataProperties {
    
    private ZerodhaProperties zerodha;
    
    public ZerodhaProperties getZerodha() {
        return zerodha;
    }
    
    public void setZerodha(ZerodhaProperties zerodha) {
        this.zerodha = zerodha;
    }
    
    /**
     * Zerodha API specific configuration properties
     */
    public static class ZerodhaProperties {
        private ApiProperties api;
        private TickerProperties ticker;
        private MarketProperties market;
        
        public ApiProperties getApi() {
            return api;
        }
        
        public void setApi(ApiProperties api) {
            this.api = api;
        }
        
        public TickerProperties getTicker() {
            return ticker;
        }
        
        public void setTicker(TickerProperties ticker) {
            this.ticker = ticker;
        }
        
        public MarketProperties getMarket() {
            return market;
        }
        
        public void setMarket(MarketProperties market) {
            this.market = market;
        }
        
        /**
         * API configuration properties
         */
        public static class ApiProperties {
            private String key;
            private String secret;
            private String accessToken;
            private MaxProperties max;
            private RetryProperties retry;
            private ThreadProperties thread;
            
            public String getKey() {
                return key;
            }
            
            public void setKey(String key) {
                this.key = key;
            }
            
            public String getSecret() {
                return secret;
            }
            
            public void setSecret(String secret) {
                this.secret = secret;
            }
            
            public String getAccessToken() {
                return accessToken;
            }
            
            public void setAccessToken(String accessToken) {
                this.accessToken = accessToken;
            }
            
            public MaxProperties getMax() {
                return max;
            }
            
            public void setMax(MaxProperties max) {
                this.max = max;
            }
            
            public RetryProperties getRetry() {
                return retry;
            }
            
            public void setRetry(RetryProperties retry) {
                this.retry = retry;
            }
            
            public ThreadProperties getThread() {
                return thread;
            }
            
            public void setThread(ThreadProperties thread) {
                this.thread = thread;
            }
            
            public static class MaxProperties {
                private int retries;
                
                public int getRetries() {
                    return retries;
                }
                
                public void setRetries(int retries) {
                    this.retries = retries;
                }
            }
            
            public static class RetryProperties {
                private DelayProperties delay;
                private MaxProperties max;
                
                public DelayProperties getDelay() {
                    return delay;
                }
                
                public void setDelay(DelayProperties delay) {
                    this.delay = delay;
                }
                
                public MaxProperties getMax() {
                    return max;
                }
                
                public void setMax(MaxProperties max) {
                    this.max = max;
                }
                
                public static class DelayProperties {
                    private int ms;
                    
                    public int getMs() {
                        return ms;
                    }
                    
                    public void setMs(int ms) {
                        this.ms = ms;
                    }
                }
                
                public static class MaxProperties {
                    private DurationProperties duration;
                    
                    public DurationProperties getDuration() {
                        return duration;
                    }
                    
                    public void setDuration(DurationProperties duration) {
                        this.duration = duration;
                    }
                    
                    public static class DurationProperties {
                        private int ms;
                        
                        public int getMs() {
                            return ms;
                        }
                        
                        public void setMs(int ms) {
                            this.ms = ms;
                        }
                    }
                }
            }
            
            public static class ThreadProperties {
                private PoolProperties pool;
                private QueueProperties queue;
                
                public PoolProperties getPool() {
                    return pool;
                }
                
                public void setPool(PoolProperties pool) {
                    this.pool = pool;
                }
                
                public QueueProperties getQueue() {
                    return queue;
                }
                
                public void setQueue(QueueProperties queue) {
                    this.queue = queue;
                }
                
                public static class PoolProperties {
                    private int size;
                    
                    public int getSize() {
                        return size;
                    }
                    
                    public void setSize(int size) {
                        this.size = size;
                    }
                }
                
                public static class QueueProperties {
                    private int capacity;
                    
                    public int getCapacity() {
                        return capacity;
                    }
                    
                    public void setCapacity(int capacity) {
                        this.capacity = capacity;
                    }
                }
            }
        }
        
        /**
         * Ticker configuration properties
         */
        public static class TickerProperties {
            private boolean enabled;
            private ReconnectProperties reconnect;
            
            public boolean isEnabled() {
                return enabled;
            }
            
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
            
            public ReconnectProperties getReconnect() {
                return reconnect;
            }
            
            public void setReconnect(ReconnectProperties reconnect) {
                this.reconnect = reconnect;
            }
            
            public static class ReconnectProperties {
                private MaxProperties max;
                private int interval;
                
                public MaxProperties getMax() {
                    return max;
                }
                
                public void setMax(MaxProperties max) {
                    this.max = max;
                }
                
                public int getInterval() {
                    return interval;
                }
                
                public void setInterval(int interval) {
                    this.interval = interval;
                }
                
                public static class MaxProperties {
                    private int retries;
                    
                    public int getRetries() {
                        return retries;
                    }
                    
                    public void setRetries(int retries) {
                        this.retries = retries;
                    }
                }
            }
        }
        
        /**
         * Market configuration properties
         */
        public static class MarketProperties {
            private DataProperties data;
            private HoursProperties hours;
            private String timezone;
            private InstrumentsProperties instruments;
            
            public DataProperties getData() {
                return data;
            }
            
            public void setData(DataProperties data) {
                this.data = data;
            }
            
            public HoursProperties getHours() {
                return hours;
            }
            
            public void setHours(HoursProperties hours) {
                this.hours = hours;
            }
            
            public String getTimezone() {
                return timezone;
            }
            
            public void setTimezone(String timezone) {
                this.timezone = timezone;
            }
            
            public InstrumentsProperties getInstruments() {
                return instruments;
            }
            
            public void setInstruments(InstrumentsProperties instruments) {
                this.instruments = instruments;
            }
            
            public static class DataProperties {
                private boolean enabled;
                
                public boolean isEnabled() {
                    return enabled;
                }
                
                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }
            }
            
            public static class HoursProperties {
                private String start;
                private String end;
                
                public String getStart() {
                    return start;
                }
                
                public void setStart(String start) {
                    this.start = start;
                }
                
                public String getEnd() {
                    return end;
                }
                
                public void setEnd(String end) {
                    this.end = end;
                }
            }
            
            public static class InstrumentsProperties {
                private String nse;
                private String bse;
                private String indices;
                
                public String getNse() {
                    return nse;
                }
                
                public void setNse(String nse) {
                    this.nse = nse;
                }
                
                public String getBse() {
                    return bse;
                }
                
                public void setBse(String bse) {
                    this.bse = bse;
                }
                
                public String getIndices() {
                    return indices;
                }
                
                public void setIndices(String indices) {
                    this.indices = indices;
                }
            }
        }
    }
}
